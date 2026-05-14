package com.android.rut.miit.productinventory.domain.model.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity

data class BarcodeProductDraft(
    val barcode: String,
    val name: String?,
    val brand: String?,
    val packageQuantity: Quantity?,
    val ingredients: String?,
    val nutrition: NutritionFacts?,
    val category: ProductCategory?,
    val source: BarcodeProductSource,
    val confidence: Double
) {
    init {
        require(barcode.isNotBlank()) { "Barcode is required" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
    }

    fun withCategory(suggestion: CategorySuggestion): BarcodeProductDraft =
        copy(
            category = suggestion.category,
            confidence = maxOf(confidence, suggestion.confidence)
        )
}

data class NutritionFacts(
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    val carbohydratesGrams: Double?
)

data class CategorySuggestion(
    val category: ProductCategory,
    val confidence: Double,
    val source: CategorySuggestionSource
) {
    init {
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
    }
}

enum class BarcodeProductSource {
    LOCAL_CACHE,
    OPEN_FOOD_FACTS,
    GS1,
    LOCAL_DATABASE
}

enum class CategorySuggestionSource {
    EXISTING,
    RULE_BASED,
    GIGACHAT,
    FALLBACK
}
