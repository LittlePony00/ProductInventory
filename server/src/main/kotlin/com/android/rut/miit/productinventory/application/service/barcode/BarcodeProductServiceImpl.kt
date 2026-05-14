package com.android.rut.miit.productinventory.application.service.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestionSource
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import org.springframework.stereotype.Service

@Service
class BarcodeProductServiceImpl(
    private val cacheRepository: IBarcodeProductCacheRepository,
    providers: List<IBarcodeProductProvider>,
    private val categorySuggestionService: CategorySuggestionService
) : IBarcodeProductService {

    private val providerChain = providers.sortedBy { it.order.ordinal }

    override fun getProductDraft(barcode: String): BarcodeProductDraft {
        val normalizedBarcode = barcode.trim()
        require(normalizedBarcode.isNotBlank()) { "Barcode is required" }

        val cachedDraft = cacheRepository.findByBarcode(normalizedBarcode)
        if (cachedDraft != null) {
            return cachedDraft.copy(source = BarcodeProductSource.LOCAL_CACHE)
        }

        val draft = providerChain
            .firstNotNullOfOrNull { it.findDraft(normalizedBarcode) }
            ?: emptyDraft(normalizedBarcode)

        val categorizedDraft = if (draft.category == null) {
            draft.withCategory(categorySuggestionService.suggestCategory(draft))
        } else {
            draft
        }

        return cacheRepository.save(categorizedDraft)
    }

    private fun emptyDraft(barcode: String): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = barcode,
            name = null,
            brand = null,
            packageQuantity = null,
            ingredients = null,
            nutrition = null,
            category = null,
            source = BarcodeProductSource.LOCAL_DATABASE,
            confidence = 0.2
        )
}

@Service
class CategorySuggestionService(
    private val gigaChatClient: IGigaChatCategoryClient
) {
    fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion =
        ruleBasedSuggestion(draft)
            ?: gigaChatClient.suggestCategory(draft)
            ?: CategorySuggestion(ProductCategory.OTHER, 0.2, CategorySuggestionSource.FALLBACK)

    private fun ruleBasedSuggestion(draft: BarcodeProductDraft): CategorySuggestion? {
        val text = listOfNotNull(draft.name, draft.brand, draft.ingredients)
            .joinToString(" ")
            .lowercase()

        return Rule.entries
            .firstOrNull { rule -> rule.keywords.any(text::contains) }
            ?.let { CategorySuggestion(it.category, it.confidence, CategorySuggestionSource.RULE_BASED) }
    }
}

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
