package com.android.rut.miit.productinventory.application.service.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeProductProviderOrder
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BarcodeProductServiceImplTest {

    @Test
    fun `returns cached draft before provider chain`() {
        val cache = InMemoryCache(
            draft(source = BarcodeProductSource.OPEN_FOOD_FACTS, category = ProductCategory.DAIRY)
        )
        val provider = StaticProvider(draft(source = BarcodeProductSource.GS1, category = ProductCategory.OTHER))
        val service = BarcodeProductServiceImpl(
            cacheRepository = cache,
            providers = listOf(provider),
            categorySuggestionService = CategorySuggestionService(TestGigaChatClient(null))
        )

        val result = service.getProductDraft("4601234567890")

        assertEquals(BarcodeProductSource.LOCAL_CACHE, result.source)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun `walks configured provider chain and caches first hit`() {
        val cache = InMemoryCache()
        val service = BarcodeProductServiceImpl(
            cacheRepository = cache,
            providers = listOf(
                StaticProvider(null, sourceName = "open-food-facts"),
                StaticProvider(draft(source = BarcodeProductSource.GS1, category = ProductCategory.BEVERAGES), "gs1")
            ),
            categorySuggestionService = CategorySuggestionService(TestGigaChatClient(null))
        )

        val result = service.getProductDraft("4601234567890")

        assertEquals(BarcodeProductSource.GS1, result.source)
        assertEquals(ProductCategory.BEVERAGES, cache.saved?.category)
    }

    @Test
    fun `adds category suggestion when provider draft has no category`() {
        val cache = InMemoryCache()
        val service = BarcodeProductServiceImpl(
            cacheRepository = cache,
            providers = listOf(
                StaticProvider(draft(name = "Сок яблочный", source = BarcodeProductSource.OPEN_FOOD_FACTS, category = null))
            ),
            categorySuggestionService = CategorySuggestionService(TestGigaChatClient(null))
        )

        val result = service.getProductDraft("4601234567890")

        assertEquals(ProductCategory.BEVERAGES, result.category)
        assertNotNull(cache.saved)
    }

    @Test
    fun `adds fallback category when no provider returns draft`() {
        val cache = InMemoryCache()
        val service = BarcodeProductServiceImpl(
            cacheRepository = cache,
            providers = emptyList(),
            categorySuggestionService = CategorySuggestionService(TestGigaChatClient(null))
        )

        val result = service.getProductDraft("4601234567890")

        assertEquals(ProductCategory.OTHER, result.category)
        assertEquals(BarcodeProductSource.LOCAL_DATABASE, result.source)
        assertEquals(ProductCategory.OTHER, cache.saved?.category)
    }

    private fun draft(
        name: String = "Product",
        source: BarcodeProductSource,
        category: ProductCategory?
    ): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = "4601234567890",
            name = name,
            brand = "Brand",
            packageQuantity = null,
            ingredients = null,
            nutrition = null,
            category = category,
            source = source,
            confidence = 0.7
        )
}

private class InMemoryCache(
    private val cached: BarcodeProductDraft? = null
) : IBarcodeProductCacheRepository {
    var saved: BarcodeProductDraft? = null

    override fun findByBarcode(barcode: String): BarcodeProductDraft? = cached

    override fun save(draft: BarcodeProductDraft): BarcodeProductDraft {
        saved = draft
        return draft
    }
}

private class StaticProvider(
    private val draft: BarcodeProductDraft?,
    sourceName: String = "open-food-facts"
) : IBarcodeProductProvider {
    override val order: BarcodeProductProviderOrder = when (sourceName) {
        "gs1" -> BarcodeProductProviderOrder.GS1
        "local-database" -> BarcodeProductProviderOrder.LOCAL_DATABASE
        else -> BarcodeProductProviderOrder.OPEN_FOOD_FACTS
    }

    var callCount: Int = 0
        private set

    override fun findDraft(barcode: String): BarcodeProductDraft? {
        callCount += 1
        return draft
    }
}

private class TestGigaChatClient(
    private val suggestion: CategorySuggestion?
) : IGigaChatCategoryClient {
    override fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion? = suggestion
}
