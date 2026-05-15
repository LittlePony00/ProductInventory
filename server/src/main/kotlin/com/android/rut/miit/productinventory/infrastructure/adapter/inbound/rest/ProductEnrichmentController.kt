package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.ProductEnrichmentSuggestionRequest
import com.android.rut.miit.productinventory.application.dto.response.ProductEnrichmentSuggestionResponse
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput
import com.android.rut.miit.productinventory.domain.port.inbound.IProductEnrichmentService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/products/enrichment")
class ProductEnrichmentController(
    private val productEnrichmentService: IProductEnrichmentService
) {
    @PostMapping("/suggest")
    fun suggestProduct(
        @PathVariable householdId: UUID,
        @RequestBody request: ProductEnrichmentSuggestionRequest
    ): ProductEnrichmentSuggestionResponse {
        val suggestion = productEnrichmentService.suggestProduct(
            userId = currentUserId(),
            householdId = householdId,
            input = ProductEnrichmentInput(
                name = request.name,
                brand = request.brand,
                barcode = request.barcode,
                ingredientsText = request.ingredientsText
            )
        )
        return ProductEnrichmentSuggestionResponse(
            categoryId = suggestion.categoryId,
            category = suggestion.category,
            categoryName = suggestion.categoryName,
            confidence = suggestion.confidence,
            source = suggestion.source,
            suggestedName = suggestion.suggestedName,
            suggestedBrand = suggestion.suggestedBrand,
            suggestedIngredientsText = suggestion.suggestedIngredientsText,
            calories = suggestion.calories,
            protein = suggestion.protein,
            fat = suggestion.fat,
            carbs = suggestion.carbs
        )
    }
}
