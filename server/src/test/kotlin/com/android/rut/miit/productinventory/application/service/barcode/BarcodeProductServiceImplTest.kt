package com.android.rut.miit.productinventory.application.service.barcode

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeProductProviderOrder
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.android.rut.miit.productinventory.domain.service.ProductCategoryRuleMatcher
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BarcodeProductServiceImplTest {

    private val userId = UUID.randomUUID()
    private val householdId = UUID.randomUUID()

    @Test
    fun `returns cached draft before provider chain`() {
        val cache = InMemoryCache(
            draft(source = BarcodeProductSource.OPEN_FOOD_FACTS, category = ProductCategory.DAIRY)
        )
        val provider = StaticProvider(draft(source = BarcodeProductSource.GS1, category = ProductCategory.OTHER))
        val service = service(cache, listOf(provider))

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(BarcodeProductSource.LOCAL_CACHE, result.source)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun `returns cached local database draft before provider chain`() {
        val cache = InMemoryCache(
            draft(source = BarcodeProductSource.LOCAL_DATABASE, category = ProductCategory.DAIRY)
        )
        val provider = StaticProvider(draft(source = BarcodeProductSource.GS1, category = ProductCategory.OTHER), "gs1")
        val service = service(cache, listOf(provider))

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(BarcodeProductSource.LOCAL_CACHE, result.source)
        assertEquals(ProductCategory.DAIRY, result.category)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun `denies non-member before lookup`() {
        val cache = InMemoryCache()
        val provider = StaticProvider(draft(source = BarcodeProductSource.OPEN_FOOD_FACTS, category = ProductCategory.DAIRY))
        val service = service(
            cache = cache,
            providers = listOf(provider),
            membershipRepository = FakeMembershipRepository(emptyList())
        )

        assertFailsWith<AccessDeniedException> {
            service.getProductDraft(userId, householdId, "4601234567890")
        }
        assertEquals(0, provider.callCount)
        assertNull(cache.saved)
    }

    @Test
    fun `walks configured provider chain and caches first external hit`() {
        val cache = InMemoryCache()
        val service = service(
            cache = cache,
            providers = listOf(
                StaticProvider(null, sourceName = "open-food-facts"),
                StaticProvider(draft(source = BarcodeProductSource.GS1, category = ProductCategory.BEVERAGES), "gs1")
            )
        )

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(BarcodeProductSource.GS1, result.source)
        assertEquals(ProductCategory.BEVERAGES, cache.saved?.category)
    }

    @Test
    fun `adds category suggestion when provider draft has no category`() {
        val cache = InMemoryCache()
        val service = service(
            cache = cache,
            providers = listOf(
                StaticProvider(draft(name = "Сок яблочный", source = BarcodeProductSource.OPEN_FOOD_FACTS, category = null))
            )
        )

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(ProductCategory.BEVERAGES, result.category)
        assertNotNull(cache.saved)
    }

    @Test
    fun `adds fallback category when no provider returns draft without caching miss`() {
        val cache = InMemoryCache()
        val service = service(cache = cache, providers = emptyList())

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(ProductCategory.OTHER, result.category)
        assertEquals(BarcodeProductSource.LOCAL_DATABASE, result.source)
        assertNull(cache.saved)
    }

    @Test
    fun `provider miss does not prevent future provider hit`() {
        val cache = InMemoryCache()
        val provider = SequenceProvider(
            drafts = listOf(
                null,
                draft(source = BarcodeProductSource.OPEN_FOOD_FACTS, category = ProductCategory.DAIRY)
            )
        )
        val service = service(cache = cache, providers = listOf(provider))

        val miss = service.getProductDraft(userId, householdId, "4601234567890")
        val hit = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(BarcodeProductSource.LOCAL_DATABASE, miss.source)
        assertEquals(BarcodeProductSource.OPEN_FOOD_FACTS, hit.source)
        assertEquals(2, provider.callCount)
        assertEquals(BarcodeProductSource.OPEN_FOOD_FACTS, cache.saved?.source)
    }

    @Test
    fun `passes user and household context to provider`() {
        val provider = StaticProvider(
            draft(source = BarcodeProductSource.LOCAL_DATABASE, category = ProductCategory.DAIRY),
            sourceName = "local-database"
        )
        val service = service(cache = InMemoryCache(), providers = listOf(provider))

        service.getProductDraft(userId, householdId, " 4601234567890 ")

        assertEquals(BarcodeLookupContext(userId, householdId, "4601234567890"), provider.lastContext)
    }

    @Test
    fun `saves local database provider result into global cache`() {
        val cache = InMemoryCache()
        val service = service(
            cache = cache,
            providers = listOf(
                StaticProvider(
                    draft(source = BarcodeProductSource.LOCAL_DATABASE, category = ProductCategory.DAIRY),
                    sourceName = "local-database"
                )
            )
        )

        val result = service.getProductDraft(userId, householdId, "4601234567890")

        assertEquals(BarcodeProductSource.LOCAL_DATABASE, result.source)
        assertEquals(BarcodeProductSource.LOCAL_DATABASE, cache.saved?.source)
        assertEquals(ProductCategory.DAIRY, cache.saved?.category)
    }

    private fun service(
        cache: InMemoryCache,
        providers: List<IBarcodeProductProvider>,
        membershipRepository: IMembershipRepository = FakeMembershipRepository(
            listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.MEMBER))
        )
    ): BarcodeProductServiceImpl =
        BarcodeProductServiceImpl(
            cacheRepository = cache,
            membershipRepository = membershipRepository,
            providers = providers,
            categorySuggestionService = CategorySuggestionService(TestGigaChatClient(null), ProductCategoryRuleMatcher())
        )

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

    var lastContext: BarcodeLookupContext? = null
        private set

    override fun findDraft(context: BarcodeLookupContext): BarcodeProductDraft? {
        callCount += 1
        lastContext = context
        return draft
    }
}

private class SequenceProvider(
    private val drafts: List<BarcodeProductDraft?>
) : IBarcodeProductProvider {
    override val order: BarcodeProductProviderOrder = BarcodeProductProviderOrder.OPEN_FOOD_FACTS

    var callCount: Int = 0
        private set

    override fun findDraft(context: BarcodeLookupContext): BarcodeProductDraft? =
        drafts.getOrNull(callCount).also { callCount += 1 }
}

private class FakeMembershipRepository(
    private val memberships: List<Membership>
) : IMembershipRepository {
    override fun findByUserId(userId: UUID): List<Membership> =
        memberships.filter { it.userId == userId }

    override fun findByHouseholdId(householdId: UUID): List<Membership> =
        memberships.filter { it.householdId == householdId }

    override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
        memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

    override fun save(membership: Membership): Membership = membership

    override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
}

private class TestGigaChatClient(
    private val suggestion: CategorySuggestion?
) : IGigaChatCategoryClient {
    override fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion? = suggestion
}
