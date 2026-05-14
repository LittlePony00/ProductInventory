package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class GetRecipeSuggestionsUseCaseTest {

    @Test
    fun `delegates recipe suggestions to repository`() = runTest {
        val recipes = listOf(recipe())
        val repository = object : RecipeRepository {
            override suspend fun getRecipes(householdId: String): List<Recipe> = emptyList()
            override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> = recipes
        }

        assertEquals(recipes, GetRecipeSuggestionsUseCase(repository)("household-id"))
    }

    private fun recipe(): Recipe =
        Recipe(
            id = "recipe-id",
            title = "Omelette",
            description = "Fast breakfast",
            ingredients = listOf("Eggs"),
            instructions = "Cook",
            imageUrl = null
        )
}
