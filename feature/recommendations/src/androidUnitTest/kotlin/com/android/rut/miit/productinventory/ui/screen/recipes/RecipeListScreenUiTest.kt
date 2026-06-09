package com.android.rut.miit.productinventory.ui.screen.recipes

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.android.rut.miit.productinventory.feature.recommendations.api.FindRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetLikedRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeIngredientOptionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.SetRecipeLikedUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.presentation.RecipeListViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeListScreenUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyStateShowsRecipeActionsAndMessage() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = viewModel(RecordingRecipeRepository(recipes = emptyList()))
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Нет подходящих рецептов").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Рецепты").assertIsDisplayed()
        compose.onAllNodesWithText("Найти рецепт")[0].assertIsDisplayed()
        compose.onNodeWithText("Нет подходящих рецептов").assertIsDisplayed()
    }

    @Test
    fun aiRecipeCardShowsAiBadgesAndUnknownCalories() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = viewModel(
                        RecordingRecipeRepository(
                            recipes = listOf(
                                recipe(
                                    title = "AI Supper",
                                    calories = 0,
                                    caloriesKnown = false,
                                    aiAssisted = true,
                                    aiGenerated = true
                                )
                            )
                        )
                    )
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("AI Supper").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("AI Supper").assertIsDisplayed()
        compose.onNodeWithText("ИИ-рецепт").assertIsDisplayed()
        compose.onNodeWithText("AI-Assisted").assertIsDisplayed()
        assertTrue(
            compose.onAllNodesWithText("15 minutes • ккал неизвестно")
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
    }

    private fun viewModel(repository: RecipeRepository): RecipeListViewModel =
        RecipeListViewModel(
            getRecipesUseCase = GetRecipesUseCase(repository),
            getRecipeSuggestionsUseCase = GetRecipeSuggestionsUseCase(repository),
            getRecipeIngredientOptionsUseCase = GetRecipeIngredientOptionsUseCase(repository),
            findRecipesUseCase = FindRecipesUseCase(repository),
            getLikedRecipesUseCase = GetLikedRecipesUseCase(repository),
            setRecipeLikedUseCase = SetRecipeLikedUseCase(repository)
        )

    private class RecordingRecipeRepository(
        private val recipes: List<Recipe>
    ) : RecipeRepository {
        override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> = recipes
        override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> = emptyList()
        override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> = emptyList()
        override suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe> = emptyList()
        override suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe =
            Recipe(
                title = "Rice Bowl",
                ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
                steps = listOf("Cook rice"),
                time = "15 minutes",
                calories = 300
            )
        override suspend fun getLikedRecipes(householdId: String): List<Recipe> = emptyList()
        override suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> = emptyList()
    }

    private fun recipe(
        title: String = "Rice Bowl",
        calories: Int = 300,
        caloriesKnown: Boolean = true,
        aiAssisted: Boolean = false,
        aiGenerated: Boolean = false
    ): Recipe =
        Recipe(
            title = title,
            ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = calories,
            caloriesKnown = caloriesKnown,
            aiAssisted = aiAssisted,
            aiGenerated = aiGenerated
        )
}
