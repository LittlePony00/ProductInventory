package com.android.rut.miit.productinventory.domain.model

import java.util.UUID

data class ProductEnrichmentInput(
    val name: String?,
    val brand: String?,
    val barcode: String?,
    val ingredientsText: String?
) {
    val normalizedText: String =
        listOfNotNull(name, brand, barcode, ingredientsText)
            .joinToString(" ")
            .lowercase()
}

data class ProductEnrichmentSuggestion(
    val categoryId: UUID,
    val category: ProductCategory,
    val categoryName: String,
    val confidence: Double,
    val source: ProductEnrichmentSource,
    val suggestedName: String? = null,
    val suggestedBrand: String? = null,
    val suggestedIngredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
) {
    init {
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        calories?.let { requireNonNegative(it, "Calories") }
        protein?.let { requireNonNegative(it, "Protein") }
        fat?.let { requireNonNegative(it, "Fat") }
        carbs?.let { requireNonNegative(it, "Carbs") }
    }
}

data class ProductEnrichmentCategoryOption(
    val id: UUID,
    val code: ProductCategory?,
    val name: String,
    val system: Boolean
)

data class AiProductEnrichmentSuggestion(
    val categoryId: UUID?,
    val category: ProductCategory?,
    val categoryName: String?,
    val confidence: Double?,
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

private fun requireNonNegative(value: Double, name: String) {
    require(value >= 0.0) { "$name must be non-negative" }
}
