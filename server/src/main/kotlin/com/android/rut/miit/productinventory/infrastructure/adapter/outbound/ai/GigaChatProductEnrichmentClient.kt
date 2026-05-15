package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai

import com.android.rut.miit.productinventory.domain.model.AiProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentCategoryOption
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput
import com.android.rut.miit.productinventory.domain.port.outbound.IProductEnrichmentClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

@Component
class GigaChatProductEnrichmentClient(
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: AiRateLimiter,
    private val accessTokenProvider: GigaChatAccessTokenProvider,
    @Value("\${gigachat.base-url:}") baseUrl: String,
    @param:Value("\${gigachat.retry-attempts:2}") private val retryAttempts: Int,
    @param:Value("\${gigachat.retry-backoff-ms:250}") private val retryBackoffMs: Long
) : IProductEnrichmentClient {

    private val log = LoggerFactory.getLogger(GigaChatProductEnrichmentClient::class.java)
    private val restClient = baseUrl.takeIf(String::isNotBlank)?.let { restClientBuilder.baseUrl(it).build() }

    override fun suggestProduct(
        input: ProductEnrichmentInput,
        categories: List<ProductEnrichmentCategoryOption>
    ): AiProductEnrichmentSuggestion? {
        val client = restClient ?: return null
        if (!rateLimiter.tryAcquire()) {
            log.warn("GigaChat product enrichment skipped: rate limit exceeded")
            return null
        }
        val accessToken = accessTokenProvider.getAccessToken() ?: return null

        return retrying {
            val response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer $accessToken")
                .body(GigaChatProductEnrichmentRequest.from(buildPrompt(input, categories)))
                .retrieve()
                .body(JsonNode::class.java)
                ?: return@retrying null

            parseSuggestion(response)?.also {
                log.info("GigaChat product enrichment succeeded")
            }
        }
    }

    private fun retrying(block: () -> AiProductEnrichmentSuggestion?): AiProductEnrichmentSuggestion? {
        repeat(retryAttempts.coerceAtLeast(1)) { attempt ->
            try {
                return block()
            } catch (exception: RestClientException) {
                if (attempt == retryAttempts.coerceAtLeast(1) - 1) {
                    log.warn("GigaChat product enrichment failed: {}", exception.message)
                    return null
                }
                Thread.sleep(retryBackoffMs.coerceAtLeast(0))
            } catch (exception: RuntimeException) {
                log.warn("GigaChat product enrichment response is invalid: {}", exception.message)
                return null
            }
        }
        return null
    }

    private fun parseSuggestion(response: JsonNode): AiProductEnrichmentSuggestion? {
        val content = response.at("/choices/0/message/content")
            .takeUnless(JsonNode::isMissingNode)
            ?.asText()
        val suggestionNode = content
            ?.let { objectMapper.readTree(it).firstObjectNode() }
            ?: response.firstObjectNode()

        return objectMapper.treeToValue(suggestionNode, GigaChatProductEnrichmentResponse::class.java).toDomain()
    }

    private fun buildPrompt(
        input: ProductEnrichmentInput,
        categories: List<ProductEnrichmentCategoryOption>
    ): String =
        """
        Return only valid JSON with fields:
        categoryId, category, categoryName, confidence, suggestedName, suggestedBrand, suggestedIngredientsText, calories, protein, fat, carbs.
        Pick one category from the available category list. Use null for unknown nutrition. confidence must be 0..1.

        Product:
        name=${input.name.orEmpty()}
        brand=${input.brand.orEmpty()}
        barcode=${input.barcode.orEmpty()}
        ingredients=${input.ingredientsText.orEmpty()}

        Available categories:
        ${categories.joinToString(separator = "\n") { "- id=${it.id}; code=${it.code?.name.orEmpty()}; name=${it.name}; system=${it.system}" }}
        """.trimIndent()
}

private fun JsonNode.firstObjectNode(): JsonNode =
    when {
        isObject -> this
        isArray && size() > 0 && first().isObject -> first()
        else -> error("Expected suggestion object")
    }

private data class GigaChatProductEnrichmentRequest(
    val model: String,
    val messages: List<GigaChatMessage>,
    val temperature: Double
) {
    companion object {
        fun from(prompt: String): GigaChatProductEnrichmentRequest =
            GigaChatProductEnrichmentRequest(
                model = "GigaChat",
                messages = listOf(
                    GigaChatMessage(
                        role = "system",
                        content = "You enrich household inventory product data. Output strictly valid JSON and no prose."
                    ),
                    GigaChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.1
            )
    }
}

private data class GigaChatMessage(
    val role: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class GigaChatProductEnrichmentResponse(
    val categoryId: UUID? = null,
    val category: ProductCategory? = null,
    val categoryName: String? = null,
    val confidence: Double? = null,
    val suggestedName: String? = null,
    val suggestedBrand: String? = null,
    val suggestedIngredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
) {
    fun toDomain(): AiProductEnrichmentSuggestion =
        AiProductEnrichmentSuggestion(
            categoryId = categoryId,
            category = category,
            categoryName = categoryName,
            confidence = confidence,
            suggestedName = suggestedName,
            suggestedBrand = suggestedBrand,
            suggestedIngredientsText = suggestedIngredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs
        )
}
