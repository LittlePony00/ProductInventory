package com.android.rut.miit.productinventory.application.service.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestionSource
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import kotlin.test.Test
import kotlin.test.assertEquals

class CategorySuggestionServiceTest {

    @Test
    fun `uses rule based category before gigachat`() {
        val service = CategorySuggestionService(
            gigaChatClient = StaticGigaChatClient(
                CategorySuggestion(ProductCategory.OTHER, 0.9, CategorySuggestionSource.GIGACHAT)
            )
        )

        val suggestion = service.suggestCategory(draft(name = "Молоко 3.2%"))

        assertEquals(ProductCategory.DAIRY, suggestion.category)
        assertEquals(CategorySuggestionSource.RULE_BASED, suggestion.source)
    }

    @Test
    fun `uses gigachat when rules do not match`() {
        val service = CategorySuggestionService(
            gigaChatClient = StaticGigaChatClient(
                CategorySuggestion(ProductCategory.BEVERAGES, 0.74, CategorySuggestionSource.GIGACHAT)
            )
        )

        val suggestion = service.suggestCategory(draft(name = "Unknown product"))

        assertEquals(ProductCategory.BEVERAGES, suggestion.category)
        assertEquals(CategorySuggestionSource.GIGACHAT, suggestion.source)
    }

    @Test
    fun `uses other fallback when rules and gigachat do not match`() {
        val service = CategorySuggestionService(gigaChatClient = StaticGigaChatClient(null))

        val suggestion = service.suggestCategory(draft(name = "Unknown product"))

        assertEquals(ProductCategory.OTHER, suggestion.category)
        assertEquals(CategorySuggestionSource.FALLBACK, suggestion.source)
    }

    private fun draft(name: String): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = "4601234567890",
            name = name,
            brand = null,
            packageQuantity = null,
            ingredients = null,
            nutrition = null,
            category = null,
            source = BarcodeProductSource.OPEN_FOOD_FACTS,
            confidence = 0.5
        )
}

private class StaticGigaChatClient(
    private val suggestion: CategorySuggestion?
) : IGigaChatCategoryClient {
    override fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion? = suggestion
}
