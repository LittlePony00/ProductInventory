package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.AiRateLimiter
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.GigaChatAccessTokenProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class GigaChatRecipeProvider(
    restClientBuilder: RestClient.Builder,
    private val recipeRetriever: RecipeRetriever,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: AiRateLimiter,
    private val accessTokenProvider: GigaChatAccessTokenProvider,
    @Value("\${gigachat.base-url:}") baseUrl: String,
    @param:Value("\${gigachat.retry-attempts:2}") private val retryAttempts: Int,
    @param:Value("\${gigachat.retry-backoff-ms:250}") private val retryBackoffMs: Long
) : IRecipeProvider {

    private val log = LoggerFactory.getLogger(GigaChatRecipeProvider::class.java)
    private val restClient = baseUrl.takeIf(String::isNotBlank)?.let { restClientBuilder.baseUrl(it).build() }

    override fun findRecipes(request: RecipeGenerationRequest): List<Recipe> {
        val matches = recipeRetriever.retrieve(request.products)
        if (matches.isEmpty()) return emptyList()

        val generatedRecipe = requestFromGigaChat(request.products, matches)
        return generatedRecipe?.let(::listOf) ?: matches.map { it.document.toRecipe() }
    }

    private fun requestFromGigaChat(products: List<Product>, matches: List<RecipeDocumentMatch>): Recipe? {
        val client = restClient ?: run {
            log.info("GigaChat recipe generation skipped: base URL is not configured")
            return null
        }
        if (!rateLimiter.tryAcquire()) {
            log.warn("GigaChat recipe generation skipped: rate limit exceeded")
            return null
        }
        val accessToken = accessTokenProvider.getAccessToken() ?: run {
            log.warn("GigaChat recipe generation skipped: access token is unavailable")
            return null
        }

        return retrying {
            val response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer $accessToken")
                .body(GigaChatRecipeRequest.from(buildPrompt(products, matches)))
                .retrieve()
                .body(JsonNode::class.java)
                ?: return@retrying null

            val recipe = parseRecipe(response)
            if (recipe == null) {
                log.warn("GigaChat recipe generation returned invalid recipe JSON")
            } else {
                log.info("GigaChat recipe generation succeeded")
            }
            recipe
        }
    }

    private fun retrying(block: () -> Recipe?): Recipe? {
        repeat(retryAttempts.coerceAtLeast(1)) { attempt ->
            try {
                return block()
            } catch (exception: RestClientException) {
                if (attempt == retryAttempts.coerceAtLeast(1) - 1) {
                    log.warn("GigaChat recipe generation failed: {}", exception.message)
                    return null
                }
                Thread.sleep(retryBackoffMs.coerceAtLeast(0))
            } catch (exception: RuntimeException) {
                log.warn("GigaChat recipe response is not valid JSON recipe: {}", exception.message)
                return null
            }
        }
        return null
    }

    private fun parseRecipe(response: JsonNode): Recipe? {
        val content = response.at("/choices/0/message/content")
            .takeUnless(JsonNode::isMissingNode)
            ?.asText()
        val recipeNode = content
            ?.let { objectMapper.readTree(it.extractJsonPayload()).firstObjectNode() }
            ?: response.firstObjectNode()

        return recipeNode.toRecipeOrNull()
    }

    private fun buildPrompt(products: List<Product>, matches: List<RecipeDocumentMatch>): String =
        """
        Return only valid JSON with exactly these fields: title, ingredients, steps, time, calories.
        ingredients must be an array of objects with name and amount. steps must be an array of strings.
        Use available inventory first. Prefer products that expire soon and consider stock levels and categories.

        Inventory:
        ${products.joinToString(separator = "\n") { it.toPromptLine() }}

        Retrieved recipes and rules:
        ${matches.joinToString(separator = "\n\n") { it.toPromptBlock() }}
        """.trimIndent()
}

private fun Product.toPromptLine(): String =
    "- $name; category=$category; remaining=$remainingAmount ${quantity.unit}; expires=${expirationDate?.date ?: "unknown"}"

private fun RecipeDocumentMatch.toPromptBlock(): String =
    """
    ${document.title}
    ingredients=${document.ingredients.joinToString { "${it.name}: ${it.amount}" }}
    steps=${document.steps.joinToString(" | ")}
    time=${document.time}
    calories=${document.calories}
    rules=${appliedRules.joinToString(" | ")}
    matched=${matchedProducts.joinToString { it.name }}
    """.trimIndent()

private fun JsonNode.firstObjectNode(): JsonNode =
    when {
        isObject -> this
        isArray && size() > 0 && first().isObject -> first()
        else -> error("Expected recipe object")
    }

private fun String.extractJsonPayload(): String {
    val trimmed = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = trimmed.indexOfFirst { it == '{' || it == '[' }
    val end = trimmed.indexOfLast { it == '}' || it == ']' }
    require(start >= 0 && end >= start) { "Expected JSON payload" }
    return trimmed.substring(start, end + 1)
}

private data class GigaChatRecipeRequest(
    val model: String,
    val messages: List<GigaChatMessage>,
    val temperature: Double
) {
    companion object {
        fun from(prompt: String): GigaChatRecipeRequest =
            GigaChatRecipeRequest(
                model = "GigaChat",
                messages = listOf(
                    GigaChatMessage(
                        role = "system",
                        content = "You are a recipe generator. Output strictly valid JSON and no prose."
                    ),
                    GigaChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.2
            )
    }
}

private data class GigaChatMessage(
    val role: String,
    val content: String
)

private fun JsonNode.toRecipeOrNull(): Recipe? {
    val safeTitle = fieldText("title") ?: return null
    val safeIngredients = path("ingredients")
        .takeIf(JsonNode::isArray)
        ?.mapNotNull { ingredient ->
            val name = ingredient.fieldText("name")
            val amount = ingredient.path("amount").valueText()
            if (name == null || amount == null) null else RecipeIngredient(name, amount)
        }
        .orEmpty()
    val safeSteps = path("steps")
        .takeIf(JsonNode::isArray)
        ?.mapNotNull(JsonNode::valueText)
        .orEmpty()
    val safeTime = path("time").valueText() ?: return null
    val safeCalories = path("calories").toNonNegativeIntOrNull() ?: return null
    if (safeIngredients.isEmpty() || safeSteps.isEmpty()) return null

    return Recipe(
        title = safeTitle,
        ingredients = safeIngredients,
        steps = safeSteps,
        time = safeTime,
        calories = safeCalories
    )
}

private fun JsonNode.fieldText(name: String): String? =
    path(name).valueText()

private fun JsonNode.valueText(): String? =
    when {
        isMissingNode || isNull -> null
        isTextual -> asText()
        isNumber || isBoolean -> asText()
        else -> null
    }?.trim()?.takeIf(String::isNotEmpty)

private fun JsonNode.toNonNegativeIntOrNull(): Int? =
    when {
        isNumber -> asDouble().takeIf { it >= 0.0 }?.toInt()
        isTextual -> numberPattern.find(asText())?.value?.toDoubleOrNull()?.takeIf { it >= 0.0 }?.toInt()
        else -> null
    }

private val numberPattern = Regex("""\d+(?:\.\d+)?""")
