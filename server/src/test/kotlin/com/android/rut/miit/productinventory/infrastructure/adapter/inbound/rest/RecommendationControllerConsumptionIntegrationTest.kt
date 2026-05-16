package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.service.ProductServiceImpl
import com.android.rut.miit.productinventory.application.service.RecommendationServiceImpl
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class RecommendationControllerConsumptionIntegrationTest {

    @AfterTest
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `recipes API excludes products depleted by consume command`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productRepository = InMemoryProductRepository(
            listOf(
                product(name = "Rice", remainingAmount = 2.0, householdId = householdId, userId = userId),
                product(name = "Milk", remainingAmount = 1.0, householdId = householdId, userId = userId)
            )
        )
        val membershipRepository = FakeMembershipRepository(
            listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
        )
        val recipeProvider = ProductEchoRecipeProvider()
        val productService = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = membershipRepository,
            notificationRepository = NoopNotificationRepository(),
            notificationSender = NoopNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository()
        )
        val controller = RecommendationController(
            RecommendationServiceImpl(
                productRepository = productRepository,
                membershipRepository = membershipRepository,
                recipeProvider = recipeProvider,
                expirationCheckService = ExpirationCheckService()
            )
        )
        authenticate(userId)

        productService.consumeProduct(userId, productRepository.productIdByName("Milk"), amount = 1.0)
        val response = controller.getRecipes(householdId)

        assertEquals(listOf("Use Rice"), response.map { it.title })
        assertEquals(listOf("Rice"), recipeProvider.requests.single().products.map { it.name })
    }

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }

    private fun product(name: String, remainingAmount: Double, householdId: UUID, userId: UUID): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            categoryId = SystemCategoryCatalog.otherId,
            quantity = Quantity(2.0, QuantityUnit.PIECES),
            remainingAmount = remainingAmount,
            householdId = householdId,
            addedByUserId = userId
        )

    private class ProductEchoRecipeProvider : IRecipeProvider {
        val requests = mutableListOf<RecipeGenerationRequest>()

        override fun findRecipes(request: RecipeGenerationRequest): List<Recipe> {
            requests += request
            return request.products.map { product ->
                Recipe(
                    title = "Use ${product.name}",
                    ingredients = listOf(RecipeIngredient(product.name, "1 serving")),
                    steps = listOf("Cook ${product.name}"),
                    time = "10 minutes",
                    calories = 100
                )
            }
        }
    }

    private class InMemoryProductRepository(initialProducts: List<Product>) : IProductRepository {
        private val products = initialProducts.associateBy { it.id }.toMutableMap()

        fun productIdByName(name: String): UUID =
            products.values.single { it.name == name }.id

        override fun findById(id: UUID): Product? = products[id]

        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
            products.values.firstOrNull { it.barcode == barcode && it.householdId == householdId }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.values.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
            products.values.filter { it.householdId == householdId && it.categoryId == categoryId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()

        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
            emptyList()

        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> = emptyList()

        override fun findLowStock(): List<Product> = emptyList()

        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> = emptyList()

        override fun save(product: Product): Product {
            products[product.id] = product
            return product
        }

        override fun deleteById(id: UUID) {
            products.remove(id)
        }

        override fun existsById(id: UUID): Boolean = products.containsKey(id)
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

    private class FakeCategoryRepository : ICategoryRepository {
        override fun findSystemCategories(includeArchived: Boolean): List<Category> =
            SystemCategoryCatalog.categories

        override fun findByHouseholdId(householdId: UUID, includeArchived: Boolean): List<Category> =
            emptyList()

        override fun findAvailableById(categoryId: UUID, householdId: UUID): Category? =
            SystemCategoryCatalog.categories.firstOrNull { it.id == categoryId }

        override fun save(category: Category): Category = category
    }

    private class NoopNotificationRepository : INotificationRepository {
        override fun findByUserId(userId: UUID): List<Notification> = emptyList()
        override fun findUnreadByUserId(userId: UUID): List<Notification> = emptyList()
        override fun existsByUserIdAndDedupeKey(userId: UUID, dedupeKey: String): Boolean = false
        override fun save(notification: Notification): Notification = notification
        override fun markAsRead(id: UUID, userId: UUID) = Unit
        override fun markAllAsRead(userId: UUID) = Unit
    }

    private class NoopNotificationSender : INotificationSender {
        override fun sendPush(userId: UUID, title: String, message: String) = Unit
    }

    private class RecordingHouseholdEventPublisher : IHouseholdEventPublisher {
        val events = mutableListOf<HouseholdEvent>()
        override fun publish(event: HouseholdEvent) {
            events += event
        }
    }
}
