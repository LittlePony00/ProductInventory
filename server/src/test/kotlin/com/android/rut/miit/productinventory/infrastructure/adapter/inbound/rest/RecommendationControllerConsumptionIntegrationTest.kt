package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.service.ProductServiceImpl
import com.android.rut.miit.productinventory.application.service.RecommendationServiceImpl
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSafetyFilter
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContextBuilder
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
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
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeGenerator
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeLocalizer
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.AiRateLimiter
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.GigaChatAccessTokenProvider
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe.GigaChatRecipeProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.hamcrest.Matchers.equalTo
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.client.RestClient

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
        val productService = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = membershipRepository,
            notificationRepository = NoopNotificationRepository(),
            notificationSender = NoopNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = NoopBarcodeProductCacheRepository()
        )
        val controller = RecommendationController(
            RecommendationServiceImpl(
                contextBuilder = RecommendationContextBuilder(
                    productRepository = productRepository,
                    membershipRepository = membershipRepository,
                    preferencesRepository = NoopFoodPreferencesRepository(),
                    expirationCheckService = ExpirationCheckService()
                ),
                safetyFilter = RecipeSafetyFilter(),
                aiRecipeGenerator = NoopAiRecipeGenerator(),
                externalRecipeSearchProviders = listOf(ProductEchoExternalRecipeProvider()),
                aiRecipeLocalizer = PassThroughRecipeLocalizer()
            )
        )
        authenticate(userId)

        productService.consumeProduct(userId, productRepository.productIdByName("Milk"), amount = 1.0)
        val response = controller.getRecipes(householdId)

        assertEquals(listOf("Use Rice"), response.map { it.title })
    }

    @Test
    fun `ai generated endpoint replaces textual unknown calories with non-zero estimate`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productRepository = InMemoryProductRepository(
            listOf(
                product(name = "Rice", remainingAmount = 1.0, householdId = householdId, userId = userId),
                product(name = "Vegetables", remainingAmount = 1.0, householdId = householdId, userId = userId)
            )
        )
        val membershipRepository = FakeMembershipRepository(
            listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
        )
        val builder = RestClient.builder()
        val gigaChatServer = MockRestServiceServer.bindTo(builder).build()
        expectGigaChatRecipeWithUnknownCalories(gigaChatServer)
        val recommendationService = RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = productRepository,
                membershipRepository = membershipRepository,
                preferencesRepository = NoopFoodPreferencesRepository(),
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = GigaChatRecipeProvider(
                restClientBuilder = builder,
                objectMapper = ObjectMapper().registerKotlinModule(),
                rateLimiter = AiRateLimiter(30),
                accessTokenProvider = GigaChatAccessTokenProvider(
                    restClientBuilder = builder,
                    apiKey = "secret",
                    oauthUrl = "https://oauth.test/token",
                    scope = "GIGACHAT_API_PERS"
                ),
                baseUrl = "https://gigachat.test",
                retryAttempts = 1,
                retryBackoffMs = 0
            )
        )
        val mockMvc = MockMvcBuilders
            .standaloneSetup(RecommendationController(recommendationService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(userId)

        mockMvc.perform(
            post("/api/v1/households/$householdId/recipes/ai-generated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title", equalTo("Рис с овощами")))
            .andExpect(jsonPath("$.calories", equalTo(240)))
            .andExpect(jsonPath("$.caloriesKnown", equalTo(true)))
            .andExpect(jsonPath("$.aiGenerated", equalTo(true)))

        gigaChatServer.verify()
    }

    @Test
    fun `search API treats empty selected product ids as random any-products search`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val externalProvider = RandomOnlyExternalRecipeProvider()
        val recommendationService = RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = InMemoryProductRepository(emptyList()),
                membershipRepository = FakeMembershipRepository(
                    listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
                ),
                preferencesRepository = NoopFoodPreferencesRepository(),
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = NoopAiRecipeGenerator(),
            externalRecipeSearchProviders = listOf(externalProvider),
            aiRecipeLocalizer = PassThroughRecipeLocalizer()
        )
        val mockMvc = MockMvcBuilders
            .standaloneSetup(RecommendationController(recommendationService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(userId)

        mockMvc.perform(
            post("/api/v1/households/$householdId/recipes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"selectedProductIds":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title", equalTo("Случайный суп")))
            .andExpect(jsonPath("$[0].usedHouseholdProducts.length()", equalTo(0)))
            .andExpect(jsonPath("$[0].missingIngredients[0]", equalTo("овощи")))

        assertEquals(emptyList(), externalProvider.stockContexts)
        assertEquals(true, externalProvider.randomContexts.single().searchesAnyProducts)
        assertEquals(emptyList(), externalProvider.randomContexts.single().candidateProducts)
    }

    @Test
    fun `search API returns random recipe list`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val externalProvider = RandomOnlyExternalRecipeProvider(
            titles = listOf("Случайный суп", "Случайная паста", "Случайный салат")
        )
        val recommendationService = RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = InMemoryProductRepository(emptyList()),
                membershipRepository = FakeMembershipRepository(
                    listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
                ),
                preferencesRepository = NoopFoodPreferencesRepository(),
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = NoopAiRecipeGenerator(),
            externalRecipeSearchProviders = listOf(externalProvider),
            aiRecipeLocalizer = PassThroughRecipeLocalizer()
        )
        val mockMvc = MockMvcBuilders
            .standaloneSetup(RecommendationController(recommendationService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(userId)

        mockMvc.perform(
            post("/api/v1/households/$householdId/recipes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"selectedProductIds":[]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()", equalTo(3)))
    }

    @Test
    fun `search API treats Russian chicken ingredient variants as selected chicken product`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productRepository = InMemoryProductRepository(
            listOf(product(name = "Курица", remainingAmount = 1.0, householdId = householdId, userId = userId))
        )
        val chickenId = productRepository.productIdByName("Курица")
        val recommendationService = RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = productRepository,
                membershipRepository = FakeMembershipRepository(
                    listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
                ),
                preferencesRepository = NoopFoodPreferencesRepository(),
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = NoopAiRecipeGenerator(),
            externalRecipeSearchProviders = listOf(
                FixedExternalRecipeProvider(
                    discoveryRecipe(
                        title = "Салат с курицей",
                        ingredients = listOf(RecipeIngredient("Куриное мясо отварное", "300 грамм"))
                    ).copy(requiresLocalization = false)
                )
            ),
            aiRecipeLocalizer = PassThroughRecipeLocalizer()
        )
        val mockMvc = MockMvcBuilders
            .standaloneSetup(RecommendationController(recommendationService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(userId)

        mockMvc.perform(
            post("/api/v1/households/$householdId/recipes/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"selectedProductIds":["$chickenId"]}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title", equalTo("Салат с курицей")))
            .andExpect(jsonPath("$[0].usedHouseholdProducts[0]", equalTo("Курица")))
            .andExpect(jsonPath("$[0].missingIngredients.length()", equalTo(0)))
    }

    private fun expectGigaChatRecipeWithUnknownCalories(server: MockRestServiceServer) {
        server.expect(requestTo("https://oauth.test/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic secret"))
            .andRespond(
                withSuccess(
                    """{"access_token":"access-token","expires_at":4102444800000}""",
                    MediaType.APPLICATION_JSON
                )
            )
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "```json\n{\"title\":\"Рис с овощами\",\"ingredients\":[{\"name\":\"rice\",\"amount\":\"1 cup\"},{\"name\":\"vegetables\",\"amount\":\"1 cup\"}],\"steps\":[\"Cook rice\",\"Cook vegetables\"],\"time\":\"20 минут\",\"calories\":\"неизвестно\"}\n```"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )
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

    private class ProductEchoExternalRecipeProvider : IExternalRecipeSearchProvider {
        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> =
            context.candidateProducts.map { product ->
                discoveryRecipe(
                    title = "Use ${product.name}",
                    ingredients = listOf(RecipeIngredient(product.name, "1 serving")),
                    steps = listOf("Cook ${product.name}")
                )
            }

        override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> = emptyList()
    }

    private class RandomOnlyExternalRecipeProvider(
        private val titles: List<String> = listOf("Случайный суп")
    ) : IExternalRecipeSearchProvider {
        val stockContexts = mutableListOf<RecommendationContext>()
        val randomContexts = mutableListOf<RecommendationContext>()

        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
            stockContexts += context
            return emptyList()
        }

        override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
            randomContexts += context
            return titles.map { title -> discoveryRecipe(title = title) }
        }
    }

    private class FixedExternalRecipeProvider(
        private val recipe: RecipeDiscoveryResult
    ) : IExternalRecipeSearchProvider {
        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> =
            listOf(recipe)
    }

    private companion object {
        fun discoveryRecipe(
            title: String,
            ingredients: List<RecipeIngredient> = listOf(RecipeIngredient("овощи", "2 стакана")),
            steps: List<String> = listOf("Сварите овощи")
        ): RecipeDiscoveryResult =
            RecipeDiscoveryResult(
                recipe = Recipe(
                    title = title,
                    ingredients = ingredients,
                    steps = steps,
                    time = "20 минут",
                    calories = 120
                ),
                source = RecipeSource.EXTERNAL_API
            )
    }

    private class NoopAiRecipeGenerator : IAiRecipeGenerator {
        override fun generateRecipe(context: AiRecipeGenerationContext): Recipe? = null
    }

    private class PassThroughRecipeLocalizer : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe = recipe
    }

    private class NoopFoodPreferencesRepository : IUserFoodPreferencesRepository {
        override fun findByUserId(userId: UUID): UserFoodPreferences? = null
        override fun save(preferences: UserFoodPreferences): UserFoodPreferences = preferences
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
        override fun sendPush(userId: UUID, title: String, message: String, notificationId: UUID?) = Unit
    }

    private class RecordingHouseholdEventPublisher : IHouseholdEventPublisher {
        val events = mutableListOf<HouseholdEvent>()
        override fun publish(event: HouseholdEvent) {
            events += event
        }
    }

    private class NoopBarcodeProductCacheRepository : IBarcodeProductCacheRepository {
        override fun findByBarcode(barcode: String): BarcodeProductDraft? = null

        override fun save(draft: BarcodeProductDraft): BarcodeProductDraft = draft
    }
}
