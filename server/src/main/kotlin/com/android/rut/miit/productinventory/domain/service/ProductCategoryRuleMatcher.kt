package com.android.rut.miit.productinventory.domain.service

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput

class ProductCategoryRuleMatcher {
    fun suggestCategory(input: ProductEnrichmentInput): RuleCategorySuggestion? =
        Rule.entries
            .firstOrNull { rule -> rule.keywords.any(input.normalizedText::contains) }
            ?.let { RuleCategorySuggestion(it.category, it.confidence) }
}

data class RuleCategorySuggestion(
    val category: ProductCategory,
    val confidence: Double
)

private enum class Rule(
    val category: ProductCategory,
    val confidence: Double,
    val keywords: Set<String>
) {
    DAIRY(
        ProductCategory.DAIRY,
        0.82,
        setOf("молоко", "кефир", "йогурт", "сыр", "творог", "сливки", "milk", "cheese", "yogurt")
    ),
    MEAT_FISH(
        ProductCategory.MEAT_FISH,
        0.82,
        setOf("мясо", "курица", "говядина", "свинина", "рыба", "лосось", "тунец", "meat", "chicken", "fish")
    ),
    VEGETABLES_FRUITS(
        ProductCategory.VEGETABLES_FRUITS,
        0.8,
        setOf("яблоко", "банан", "томат", "огурец", "овощ", "фрукт", "apple", "banana", "tomato")
    ),
    CEREALS(
        ProductCategory.CEREALS,
        0.78,
        setOf("крупа", "рис", "гречка", "хлопья", "макароны", "мука", "bread", "rice", "pasta", "cereal")
    ),
    BEVERAGES(
        ProductCategory.BEVERAGES,
        0.82,
        setOf("вода", "сок", "напиток", "чай", "кофе", "water", "juice", "drink", "tea", "coffee")
    )
}
