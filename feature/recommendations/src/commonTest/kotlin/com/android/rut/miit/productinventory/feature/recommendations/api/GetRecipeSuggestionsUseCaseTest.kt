package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetRecipeSuggestionsUseCaseTest {

    @Test
    fun `delegates recipe suggestions to repository`() = runTest {
        val recipes = listOf(recipe())
        val repository = object : RecipeRepository {
            override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> = emptyList()
            override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> = recipes
            override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> = emptyList()
            override suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe> = emptyList()
            override suspend fun generateAiRecipe(
                householdId: String,
                request: AiRecipeGenerationRequest
            ): Recipe = recipe()
            override suspend fun getLikedRecipes(householdId: String): List<Recipe> = emptyList()
            override suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> =
                emptyList()
        }

        assertEquals(recipes, GetRecipeSuggestionsUseCase(repository)("household-id"))
    }

    private fun recipe(): Recipe =
        Recipe(
            title = "Omelette",
            ingredients = listOf(RecipeIngredient(name = "Eggs", amount = "2 pieces")),
            steps = listOf("Cook"),
            time = "10 minutes",
            calories = 250
        )
}
