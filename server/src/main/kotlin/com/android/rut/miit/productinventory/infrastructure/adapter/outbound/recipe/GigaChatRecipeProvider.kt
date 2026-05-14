package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
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
    @Value("\${gigachat.base-url:}") baseUrl: String,
    @param:Value("\${gigachat.api-key:}") private val apiKey: String
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
        val client = restClient ?: return null
        if (apiKey.isBlank()) return null

        return try {
            val response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .body(GigaChatRecipeRequest.from(buildPrompt(products, matches)))
                .retrieve()
                .body(JsonNode::class.java)
                ?: return null

            parseRecipe(response)
        } catch (exception: RestClientException) {
            log.warn("GigaChat recipe generation failed: {}", exception.message)
            null
        } catch (exception: RuntimeException) {
            log.warn("GigaChat recipe response is not valid JSON recipe: {}", exception.message)
            null
        }
    }

    private fun parseRecipe(response: JsonNode): Recipe? {
        val content = response.at("/choices/0/message/content")
            .takeUnless(JsonNode::isMissingNode)
            ?.asText()
        val recipeNode = content
            ?.let { objectMapper.readTree(it).firstObjectNode() }
            ?: response.firstObjectNode()

        return objectMapper.treeToValue(recipeNode, GigaChatRecipeResponse::class.java).toDomainOrNull()
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
    rules=${appliedRules.joinToString(" | ")}
    matched=${matchedProducts.joinToString { it.name }}
    """.trimIndent()

private fun JsonNode.firstObjectNode(): JsonNode =
    when {
        isObject -> this
        isArray && size() > 0 && first().isObject -> first()
        else -> error("Expected recipe object")
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

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GigaChatRecipeResponse(
    val title: String? = null,
    val ingredients: List<RecipeIngredient>? = null,
    val steps: List<String>? = null,
    val time: String? = null,
    val calories: Int? = null
) {
    fun toDomainOrNull(): Recipe? {
        val safeTitle = title?.takeIf(String::isNotBlank) ?: return null
        val safeIngredients = ingredients?.filter { it.name.isNotBlank() && it.amount.isNotBlank() }.orEmpty()
        val safeSteps = steps?.filter(String::isNotBlank).orEmpty()
        val safeTime = time?.takeIf(String::isNotBlank) ?: return null
        val safeCalories = calories?.takeIf { it >= 0 } ?: return null
        if (safeIngredients.isEmpty() || safeSteps.isEmpty()) return null

        return Recipe(
            title = safeTitle,
            ingredients = safeIngredients,
            steps = safeSteps,
            time = safeTime,
            calories = safeCalories
        )
    }
}
