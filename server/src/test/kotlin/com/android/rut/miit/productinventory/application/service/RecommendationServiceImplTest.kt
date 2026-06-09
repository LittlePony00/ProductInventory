package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSafetyFilter
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContextBuilder
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeGenerator
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeLocalizer
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendationServiceImplTest {

    @Test
    fun `includes localized themealdb recipe and gigachat result`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            userId = userId,
            householdId = householdId,
            externalProviders = listOf(
                randomProvider(
                    externalRecipe("Овощной суп", "https://povar.ru/recipes/soup.html", requiresLocalization = false)
                ),
                randomProvider(
                    externalRecipe("TheMealDB curry", "https://www.themealdb.com/meal/52772", requiresLocalization = true)
                )
            ),
            aiSearchProvider = StaticAiSearchProvider(
                aiRecipe("Рис с овощами")
            ),
            localizer = TranslatingRecipeLocalizer(recipe("Куриное карри", "курица"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertContainsSource(recipes, "povar.ru")
        assertContainsSource(recipes, "themealdb.com")
        assertTrue(recipes.any { it.sourceName == "Povar.ru" })
        assertTrue(recipes.any { it.sourceName == "TheMealDB" })
        assertTrue(recipes.any { it.aiAssisted && it.title == "Рис с овощами" })
        assertEquals("Куриное карри", recipes.single { it.sourceUrl?.contains("themealdb.com") == true }.title)
    }

    @Test
    fun `drops english external recipe when localization fails`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            userId = userId,
            householdId = householdId,
            externalProviders = listOf(
                randomProvider(
                    externalRecipe("Chicken curry", "https://www.themealdb.com/meal/52772", requiresLocalization = true)
                )
            ),
            aiSearchProvider = StaticAiSearchProvider(emptyList()),
            localizer = RejectingRecipeLocalizer
        )

        assertEquals(emptyList(), service.findRecipes(userId, householdId, RecipeSearchRequest()))
    }

    @Test
    fun `filters disliked cucumber recipe using russian inflection`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            userId = userId,
            householdId = householdId,
            preferences = UserFoodPreferences(userId = userId, dislikedIngredients = setOf("огурцы")),
            externalProviders = listOf(
                randomProvider(
                    externalRecipe("Салат с огурцом", "https://povar.ru/recipes/cucumber.html", requiresLocalization = false, ingredient = "свежий огурец"),
                    externalRecipe("Рисовая каша", "https://povar.ru/recipes/rice.html", requiresLocalization = false, ingredient = "рис")
                )
            ),
            aiSearchProvider = StaticAiSearchProvider(emptyList()),
            localizer = RejectingRecipeLocalizer
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(listOf("Рисовая каша"), recipes.map { it.title })
    }

    @Test
    fun `does not mark stocked equivalent ingredient as missing for random gigachat recipe`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            userId = userId,
            householdId = householdId,
            products = listOf(product("Огурцы", userId, householdId)),
            externalProviders = emptyList(),
            aiSearchProvider = StaticAiSearchProvider(
                listOf(aiRecipe("Овощной салат", ingredient = "свежий огурец"))
            ),
            localizer = RejectingRecipeLocalizer
        )

        val recipe = service.findRecipes(userId, householdId, RecipeSearchRequest()).single()

        assertEquals(listOf("Огурцы"), recipe.usedHouseholdProducts)
        assertTrue("свежий огурец" !in recipe.missingIngredients)
    }

    @Test
    fun `random recipe does not mark product as used by category only`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            userId = userId,
            householdId = householdId,
            products = listOf(product("Яблоки", userId, householdId)),
            externalProviders = emptyList(),
            aiSearchProvider = StaticAiSearchProvider(
                listOf(aiRecipe("Томатный салат", ingredient = "помидор"))
            ),
            localizer = RejectingRecipeLocalizer
        )

        val recipe = service.findRecipes(userId, householdId, RecipeSearchRequest()).single()

        assertEquals(emptyList(), recipe.usedHouseholdProducts)
        assertEquals(listOf("помидор"), recipe.missingIngredients)
    }

    private fun service(
        userId: UUID,
        householdId: UUID,
        products: List<Product> = emptyList(),
        preferences: UserFoodPreferences? = null,
        externalProviders: List<IExternalRecipeSearchProvider>,
        aiSearchProvider: IAiRecipeSearchProvider,
        localizer: IAiRecipeLocalizer
    ): RecommendationServiceImpl =
        RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = StaticProductRepository(products),
                membershipRepository = StaticMembershipRepository(
                    Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER)
                ),
                preferencesRepository = StaticFoodPreferencesRepository(preferences),
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = EmptyAiRecipeGenerator,
            externalRecipeSearchProviders = externalProviders,
            aiRecipeSearchProvider = aiSearchProvider,
            aiRecipeLocalizer = localizer
        )

    private fun randomProvider(vararg recipes: RecipeDiscoveryResult): IExternalRecipeSearchProvider =
        StaticExternalRecipeSearchProvider(randomRecipes = recipes.toList())

    private fun externalRecipe(
        title: String,
        sourceUrl: String,
        requiresLocalization: Boolean,
        ingredient: String = "рис"
    ): RecipeDiscoveryResult =
        RecipeDiscoveryResult(
            recipe = recipe(title, ingredient),
            source = RecipeSource.EXTERNAL_API,
            sourceName = if ("themealdb.com" in sourceUrl) "TheMealDB" else "Povar.ru",
            sourceUrl = sourceUrl,
            reasons = listOf("source"),
            requiresLocalization = requiresLocalization
        )

    private fun aiRecipe(title: String, ingredient: String = "рис"): RecipeDiscoveryResult =
        RecipeDiscoveryResult(
            recipe = recipe(title, ingredient),
            source = RecipeSource.AI_ASSISTED,
            sourceName = "GigaChat",
            reasons = listOf("AI-Assisted"),
            aiAssisted = true
        )

    private fun recipe(title: String, ingredient: String = "рис"): Recipe =
        Recipe(
            title = title,
            ingredients = listOf(RecipeIngredient(ingredient, "100 г")),
            steps = listOf("Смешайте ингредиенты и доведите до готовности."),
            time = "20 минут",
            calories = 240
        )

    private fun product(name: String, userId: UUID, householdId: UUID): Product =
        Product(
            name = name,
            category = ProductCategory.VEGETABLES_FRUITS,
            quantity = Quantity(2.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = userId
        )

    private fun assertContainsSource(recipes: List<RecipeRecommendation>, host: String) {
        assertTrue(recipes.any { it.sourceUrl?.contains(host) == true }, "Expected recipe from $host")
    }

    private class StaticExternalRecipeSearchProvider(
        private val randomRecipes: List<RecipeDiscoveryResult>
    ) : IExternalRecipeSearchProvider {
        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> =
            randomRecipes

        override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> =
            randomRecipes
    }

    private class StaticAiSearchProvider(
        private val recipes: List<RecipeDiscoveryResult>
    ) : IAiRecipeSearchProvider {
        constructor(recipe: RecipeDiscoveryResult) : this(listOf(recipe))

        override fun searchWebRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> =
            recipes
    }

    private object RejectingRecipeLocalizer : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe? = null
        override fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> =
            recipes.map { null }
    }

    private class TranslatingRecipeLocalizer(private val translatedRecipe: Recipe) : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe = translatedRecipe
        override fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> =
            recipes.map { translatedRecipe }
    }

    private object EmptyAiRecipeGenerator : IAiRecipeGenerator {
        override fun generateRecipe(context: AiRecipeGenerationContext): Recipe? = null
    }

    private class StaticProductRepository(private val products: List<Product>) : IProductRepository {
        override fun findById(id: UUID): Product? = null
        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? = null
        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> = emptyList()
        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()
        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> = emptyList()
        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> = emptyList()

        override fun findLowStock(): List<Product> = emptyList()
        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> = emptyList()
        override fun save(product: Product): Product = product
        override fun deleteById(id: UUID) = Unit
        override fun existsById(id: UUID): Boolean = false
    }

    private class StaticMembershipRepository(private val membership: Membership) : IMembershipRepository {
        override fun findByUserId(userId: UUID): List<Membership> =
            listOf(membership).filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            listOf(membership).filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            membership.takeIf { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership = membership
        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
    }

    private class StaticFoodPreferencesRepository(
        private val preferences: UserFoodPreferences?
    ) : IUserFoodPreferencesRepository {
        override fun findByUserId(userId: UUID): UserFoodPreferences? =
            preferences?.takeIf { it.userId == userId }

        override fun save(preferences: UserFoodPreferences): UserFoodPreferences = preferences
    }
}
