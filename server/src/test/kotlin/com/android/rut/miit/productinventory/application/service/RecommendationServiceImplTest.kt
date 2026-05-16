package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecommendationServiceImplTest {

    @Test
    fun `passes expiration-prioritized product context to recipe provider`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val provider = RecordingRecipeProvider()
        val service = RecommendationServiceImpl(
            productRepository = FakeProductRepository(
                listOf(
                    product(name = "Rice", householdId = householdId, expirationDate = LocalDate.now().plusDays(7)),
                    product(name = "Yogurt", householdId = householdId, expirationDate = LocalDate.now().plusDays(1))
                )
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            recipeProvider = provider,
            expirationCheckService = ExpirationCheckService()
        )

        service.getRecipes(userId, householdId)

        assertEquals(listOf("Yogurt", "Rice"), provider.requests.single().products.map { it.name })
    }

    @Test
    fun `uses only remaining products for recipe provider context`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val provider = RecordingRecipeProvider()
        val service = RecommendationServiceImpl(
            productRepository = FakeProductRepository(
                listOf(
                    product(name = "Rice", householdId = householdId, remainingAmount = 2.0),
                    product(name = "Empty Yogurt", householdId = householdId, remainingAmount = 0.0)
                )
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            recipeProvider = provider,
            expirationCheckService = ExpirationCheckService()
        )

        service.getRecipes(userId, householdId)

        assertEquals(listOf("Rice"), provider.requests.single().products.map { it.name })
    }

    @Test
    fun `rejects users outside household`() {
        val service = RecommendationServiceImpl(
            productRepository = FakeProductRepository(emptyList()),
            membershipRepository = FakeMembershipRepository(emptyList()),
            recipeProvider = RecordingRecipeProvider(),
            expirationCheckService = ExpirationCheckService()
        )

        assertFailsWith<AccessDeniedException> {
            service.getRecipes(UUID.randomUUID(), UUID.randomUUID())
        }
    }

    private fun product(
        name: String,
        householdId: UUID,
        expirationDate: LocalDate = LocalDate.now().plusDays(7),
        remainingAmount: Double = 1.0
    ): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = remainingAmount,
            expirationDate = com.android.rut.miit.productinventory.domain.model.ExpirationDate(expirationDate),
            householdId = householdId,
            addedByUserId = UUID.randomUUID()
        )

    private class RecordingRecipeProvider : IRecipeProvider {
        val requests = mutableListOf<RecipeGenerationRequest>()

        override fun findRecipes(request: RecipeGenerationRequest): List<Recipe> {
            requests += request
            return emptyList()
        }
    }

    private class FakeProductRepository(private val products: List<Product>) : IProductRepository {
        override fun findById(id: UUID): Product? = products.firstOrNull { it.id == id }
        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
            products.firstOrNull { it.barcode == barcode && it.householdId == householdId }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
            products.filter { it.householdId == householdId && it.categoryId == categoryId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> =
            products.filter { it.householdId == householdId && it.expirationDate?.date?.isBefore(date) == true }

        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
            products.filter { product ->
                product.expirationDate?.date?.let { !it.isBefore(startInclusive) && it.isBefore(endExclusive) } == true
            }

        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> =
            findExpiringBetween(startInclusive, endExclusive).filter { it.householdId == householdId }

        override fun findLowStock(): List<Product> =
            products.filter { product ->
                product.lowStockThreshold?.let { product.remainingAmount <= it } == true
            }

        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> =
            findLowStock().filter { it.householdId == householdId }

        override fun save(product: Product): Product = product
        override fun deleteById(id: UUID) = Unit
        override fun existsById(id: UUID): Boolean = products.any { it.id == id }
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
