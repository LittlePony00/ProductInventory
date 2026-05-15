package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.AiProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentCategoryOption
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentSource
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductEnrichmentClient
import com.android.rut.miit.productinventory.domain.service.ProductCategoryRuleMatcher
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductEnrichmentServiceImplTest {

    @Test
    fun `uses gigachat suggestion when client returns available custom category`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val customCategory = Category(householdId = householdId, name = "Bakery")
        val service = service(
            userId = userId,
            householdId = householdId,
            categories = listOf(customCategory),
            clientSuggestion = AiProductEnrichmentSuggestion(
                categoryId = customCategory.id,
                category = null,
                categoryName = null,
                confidence = 0.9,
                suggestedName = "Bread",
                suggestedBrand = null,
                suggestedIngredientsText = "Wheat flour",
                calories = 250.0,
                protein = 8.0,
                fat = 2.0,
                carbs = 50.0
            )
        )

        val suggestion = service.suggestProduct(userId, householdId, ProductEnrichmentInput("Bread", null, null, null))

        assertEquals(customCategory.id, suggestion.categoryId)
        assertEquals("Bakery", suggestion.categoryName)
        assertEquals(ProductEnrichmentSource.GIGACHAT, suggestion.source)
        assertEquals("Bread", suggestion.suggestedName)
        assertEquals(250.0, suggestion.calories)
    }

    @Test
    fun `falls back to rule category when ai client is unavailable`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(userId = userId, householdId = householdId)

        val suggestion = service.suggestProduct(userId, householdId, ProductEnrichmentInput("Milk", null, null, null))

        assertEquals(SystemCategoryCatalog.dairyId, suggestion.categoryId)
        assertEquals(ProductCategory.DAIRY, suggestion.category)
        assertEquals(ProductEnrichmentSource.RULE_BASED, suggestion.source)
    }

    @Test
    fun `rejects user outside household`() {
        val service = service(userId = UUID.randomUUID(), householdId = UUID.randomUUID(), memberships = emptyList())

        assertFailsWith<AccessDeniedException> {
            service.suggestProduct(UUID.randomUUID(), UUID.randomUUID(), ProductEnrichmentInput("Milk", null, null, null))
        }
    }

    private fun service(
        userId: UUID,
        householdId: UUID,
        categories: List<Category> = emptyList(),
        memberships: List<Membership> = listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER)),
        clientSuggestion: AiProductEnrichmentSuggestion? = null
    ): ProductEnrichmentServiceImpl =
        ProductEnrichmentServiceImpl(
            membershipRepository = FakeMembershipRepository(memberships),
            categoryRepository = FakeCategoryRepository(categories),
            productEnrichmentClient = FakeProductEnrichmentClient(clientSuggestion),
            ruleMatcher = ProductCategoryRuleMatcher()
        )

    private class FakeProductEnrichmentClient(
        private val suggestion: AiProductEnrichmentSuggestion?
    ) : IProductEnrichmentClient {
        override fun suggestProduct(
            input: ProductEnrichmentInput,
            categories: List<ProductEnrichmentCategoryOption>
        ): AiProductEnrichmentSuggestion? = suggestion
    }

    private class FakeCategoryRepository(
        categories: List<Category>
    ) : ICategoryRepository {
        private val categories = (SystemCategoryCatalog.categories + categories).associateBy { it.id }.toMutableMap()

        override fun findSystemCategories(includeArchived: Boolean): List<Category> =
            categories.values.filter { it.system && (includeArchived || !it.archived) }

        override fun findByHouseholdId(householdId: UUID, includeArchived: Boolean): List<Category> =
            categories.values.filter { it.householdId == householdId && (includeArchived || !it.archived) }

        override fun findAvailableById(categoryId: UUID, householdId: UUID): Category? =
            categories[categoryId]?.takeIf { !it.archived && (it.system || it.householdId == householdId) }

        override fun save(category: Category): Category {
            categories[category.id] = category
            return category
        }
    }

    private class FakeMembershipRepository(private val memberships: List<Membership>) : IMembershipRepository {
        override fun findByUserId(userId: UUID): List<Membership> =
            memberships.filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            memberships.filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership = membership
        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
    }
}
