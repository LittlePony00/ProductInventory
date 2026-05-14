package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.gigachat

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestionSource
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class GigaChatCategoryClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${gigachat.base-url:}") baseUrl: String,
    @param:Value("\${gigachat.api-key:}") private val apiKey: String
) : IGigaChatCategoryClient {

    private val restClient = baseUrl.takeIf(String::isNotBlank)?.let {
        restClientBuilder.baseUrl(it).build()
    }

    override fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion? {
        val client = restClient ?: return null
        if (apiKey.isBlank()) return null

        return try {
            client.post()
                .uri("/category-suggestions")
                .header("Authorization", "Bearer $apiKey")
                .body(GigaChatCategoryRequest.from(draft))
                .retrieve()
                .body(GigaChatCategoryResponse::class.java)
                ?.toSuggestion()
        } catch (_: RestClientException) {
            null
        }
    }
}

private data class GigaChatCategoryRequest(
    val name: String?,
    val brand: String?,
    val ingredients: String?
) {
    companion object {
        fun from(draft: BarcodeProductDraft): GigaChatCategoryRequest =
            GigaChatCategoryRequest(draft.name, draft.brand, draft.ingredients)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GigaChatCategoryResponse(
    val category: ProductCategory? = null,
    val confidence: Double? = null
) {
    fun toSuggestion(): CategorySuggestion? {
        val safeCategory = category ?: return null
        val safeConfidence = confidence?.coerceIn(0.0, 1.0) ?: 0.65
        return CategorySuggestion(safeCategory, safeConfidence, CategorySuggestionSource.GIGACHAT)
    }
}
