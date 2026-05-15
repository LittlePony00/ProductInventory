package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentSource
import java.util.UUID

data class ProductEnrichmentSuggestionResponse(
    val categoryId: UUID,
    val category: ProductCategory,
    val categoryName: String,
    val confidence: Double,
    val source: ProductEnrichmentSource,
    val suggestedName: String?,
    val suggestedBrand: String?,
    val suggestedIngredientsText: String?,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?
)
