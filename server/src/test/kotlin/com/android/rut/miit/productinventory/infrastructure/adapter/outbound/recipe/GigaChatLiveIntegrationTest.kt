package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.AiRateLimiter
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.GigaChatAccessTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.web.client.RestClient

@Tag("live")
@EnabledIfEnvironmentVariable(named = "RUN_GIGACHAT_LIVE_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "GIGACHAT_API_KEY", matches = ".+")
class GigaChatLiveIntegrationTest {

    @Test
    @Timeout(90)
    fun `generates recipe through live GigaChat API`() {
        val userId = UUID.randomUUID()
        val provider = liveProvider(apiKey = requireNotNull(System.getenv("GIGACHAT_API_KEY")))

        val recipe = provider.generateRecipe(
            AiRecipeGenerationContext(
                products = listOf(
                    product("Рис", ProductCategory.CEREALS),
                    product("Томаты", ProductCategory.VEGETABLES_FRUITS)
                ),
                preferences = UserFoodPreferences.empty(userId).copy(
                    preferredCuisines = setOf("домашняя"),
                    maxCookingTimeMinutes = 30,
                    servings = 2
                ),
                request = AiRecipeGenerationRequest(
                    maxCookingTimeMinutes = 30,
                    servings = 2,
                    extraNotes = "Нужен простой рецепт без дополнительных пожеланий."
                )
            )
        )

        assertNotNull(recipe)
        assertTrue(recipe.title.isNotBlank(), "Generated recipe must have a title")
        assertTrue(recipe.ingredients.isNotEmpty(), "Generated recipe must include ingredients")
        assertTrue(recipe.steps.isNotEmpty(), "Generated recipe must include cooking steps")
        assertTrue(recipe.time.isNotBlank(), "Generated recipe must include cooking time")
    }

    @Test
    @Timeout(90)
    fun `localizes found recipes through live GigaChat API`() {
        val provider = liveProvider(apiKey = requireNotNull(System.getenv("GIGACHAT_API_KEY")))

        val recipes = provider.localizeAndEnrichFoundRecipes(
            listOf(
                Recipe(
                    title = "Chicken Rice",
                    ingredients = listOf(
                        RecipeIngredient("chicken", "300 g"),
                        RecipeIngredient("rice", "1 cup")
                    ),
                    steps = listOf("Cook chicken", "Add rice"),
                    time = "Unknown",
                    calories = 0,
                    caloriesKnown = false
                ),
                Recipe(
                    title = "Tomato Pasta",
                    ingredients = listOf(
                        RecipeIngredient("tomato", "2 pcs"),
                        RecipeIngredient("pasta", "200 g")
                    ),
                    steps = listOf("Boil pasta", "Add tomato sauce"),
                    time = "20 minutes",
                    calories = 0,
                    caloriesKnown = false
                )
            )
        ).filterNotNull()

        assertEquals(2, recipes.size)
        recipes.forEach { recipe ->
            assertTrue(recipe.title.isNotBlank(), "Localized recipe must have a title")
            assertTrue(recipe.ingredients.isNotEmpty(), "Localized recipe must include ingredients")
            assertTrue(recipe.steps.isNotEmpty(), "Localized recipe must include steps")
            assertTrue(recipe.time.isNotBlank(), "Localized recipe must include time")
            assertTrue(recipe.calories > 0, "Localized recipe must include non-zero calories")
        }
    }

    private fun liveProvider(apiKey: String): GigaChatRecipeProvider {
        val builder = RestClient.builder()
        return GigaChatRecipeProvider(
            restClientBuilder = builder,
            objectMapper = ObjectMapper().registerKotlinModule(),
            rateLimiter = AiRateLimiter(30),
            accessTokenProvider = GigaChatAccessTokenProvider(
                restClientBuilder = builder,
                apiKey = apiKey,
                oauthUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
                scope = "GIGACHAT_API_PERS"
            ),
            baseUrl = "https://gigachat.devices.sberbank.ru/api/v1",
            retryAttempts = 1,
            retryBackoffMs = 0
        )
    }

    private fun product(name: String, category: ProductCategory): Product =
        Product(
            name = name,
            category = category,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
}
