package com.android.rut.miit.productinventory.feature.products.api.models

data class ProductEnrichmentSuggestion(
    val categoryId: String,
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

enum class ProductEnrichmentSource {
    RULE_BASED,
    GIGACHAT,
    FALLBACK
}
