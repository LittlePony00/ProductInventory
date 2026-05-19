package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class RecipeListViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads empty saved recipe list as empty state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(recipes = emptyList())
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Empty>(viewModel.viewState.value)
            assertEquals(false, state.generated)
            assertEquals(listOf("household-id"), repository.recipeHouseholdIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `shows error state when recipe load fails`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = viewModel(FakeRecipeRepository(error = IllegalStateException("recipe backend down")))

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Error>(viewModel.viewState.value)
            assertEquals("recipe backend down", state.message)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `retry uses last household id`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(recipes = listOf(recipe()))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnRetry)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Rice Bowl"), state.recipes.map { it.title })
            assertEquals(listOf("household-id", "household-id"), repository.recipeHouseholdIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `generate uses suggestions endpoint with last household id`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(suggestions = listOf(recipe(title = "Suggested Bowl")))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnGenerateClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Suggested Bowl"), state.recipes.map { it.title })
            assertEquals(listOf("household-id"), repository.recipeHouseholdIds)
            assertEquals(listOf("household-id"), repository.suggestionHouseholdIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `generate empty suggestions shows generated empty state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(recipes = listOf(recipe()), suggestions = emptyList())
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnGenerateClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Empty>(viewModel.viewState.value)
            assertEquals(true, state.generated)
            assertEquals(listOf("household-id"), repository.suggestionHouseholdIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: RecipeRepository): RecipeListViewModel =
        RecipeListViewModel(
            getRecipesUseCase = GetRecipesUseCase(repository),
            getRecipeSuggestionsUseCase = GetRecipeSuggestionsUseCase(repository)
        )

    private class FakeRecipeRepository(
        private val recipes: List<Recipe> = emptyList(),
        private val suggestions: List<Recipe> = emptyList(),
        private val error: Throwable? = null
    ) : RecipeRepository {
        val recipeHouseholdIds = mutableListOf<String>()
        val suggestionHouseholdIds = mutableListOf<String>()

        override suspend fun getRecipes(householdId: String): List<Recipe> {
            recipeHouseholdIds += householdId
            error?.let { throw it }
            return recipes
        }

        override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> {
            suggestionHouseholdIds += householdId
            error?.let { throw it }
            return suggestions
        }
    }

    private fun recipe(title: String = "Rice Bowl"): Recipe =
        Recipe(
            title = title,
            ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300
        )
}
