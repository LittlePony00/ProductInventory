package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.feature.recommendations.api.FindRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetLikedRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeIngredientOptionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.SetRecipeLikedUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
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
            assertEquals(true, state.generated)
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
    fun `generate opens ingredient selection dialog without loading recipes`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(recipes = listOf(recipe(title = "Current Bowl")))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnGenerateClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(true, state.showIngredientDialog)
            assertEquals(listOf("Rice", "Tomato"), state.ingredientOptions.map { it.name })
            assertEquals(listOf("household-id"), repository.recipeHouseholdIds)
            assertEquals(emptyList(), repository.suggestionHouseholdIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `find selected recipe sends selected ingredient ids`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(foundRecipes = listOf(recipe(title = "Selected Bowl")))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnGenerateClick)
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnIngredientSelectionChanged("rice-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnFindSelectedRecipeClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Selected Bowl"), state.recipes.map { it.title })
            assertEquals(listOf(setOf("rice-id")), repository.recipeSearchRequests.map { it.selectedProductIds })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `random recipe search sends empty selected ids`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(foundRecipes = listOf(recipe(title = "Random Bowl")))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnGenerateClick)
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnRandomRecipeClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Random Bowl"), state.recipes.map { it.title })
            assertEquals(listOf(emptySet()), repository.recipeSearchRequests.map { it.selectedProductIds })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `liked recipes are stored locally and shown on liked tab`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val recipe = recipe(title = "Random Bowl")
            val repository = FakeRecipeRepository(recipes = listOf(recipe))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnRecipeLikeClick(recipe))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnLikedTabClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(RecipeListTab.LIKED, state.selectedTab)
            assertEquals(listOf("Random Bowl"), state.recipes.map { it.title })
            assertEquals(setOf(recipe.localIdentity()), state.likedRecipeIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `unliking recipe from liked tab removes it and shows empty liked state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val recipe = recipe(title = "Random Bowl")
            val repository = FakeRecipeRepository(recipes = listOf(recipe)).apply {
                likedRecipes = listOf(recipe)
            }
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnLikedTabClick)
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnRecipeLikeClick(recipe))
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Empty>(viewModel.viewState.value)
            assertEquals(RecipeListTab.LIKED, state.selectedTab)
            assertEquals(emptyList(), state.likedRecipes)
            assertEquals(emptySet(), state.likedRecipeIds)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `use soon loads use soon mode`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(recipes = listOf(recipe(title = "Use Soon Bowl")))
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnUseSoonClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(RecommendationMode.USE_SOON, state.selectedMode)
            assertEquals(listOf(RecommendationMode.CURRENT_PRODUCTS, RecommendationMode.USE_SOON), repository.recipeModes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `mode switch clears quick filters so valid recipes are not hidden`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRecipeRepository(
                recipes = listOf(
                    recipe(title = "Быстрый салат", time = "15 минут"),
                    recipe(title = "Долгий суп", time = "45 минут")
                )
            )
            val viewModel = viewModel(repository)

            viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(RecipeListEvent.OnQuickFilterChanged(RecipeQuickFilter.UNDER_30_MIN))
            advanceUntilIdle()

            assertEquals(
                listOf("Быстрый салат"),
                assertIs<RecipeListState.Content>(viewModel.viewState.value).recipes.map { it.title }
            )

            viewModel.onEvent(RecipeListEvent.OnUseSoonClick)
            advanceUntilIdle()

            val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
            assertEquals(emptySet(), state.quickFilters)
            assertEquals(listOf("Быстрый салат", "Долгий суп"), state.recipes.map { it.title })
            assertEquals(RecommendationMode.USE_SOON, state.selectedMode)
        } finally {
            Dispatchers.resetMain()
        }
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

    private class FakeRecipeRepository(
        private val recipes: List<Recipe> = emptyList(),
        private val suggestions: List<Recipe> = emptyList(),
        private val foundRecipes: List<Recipe> = emptyList(),
        private val error: Throwable? = null
    ) : RecipeRepository {
        val recipeHouseholdIds = mutableListOf<String>()
        val recipeModes = mutableListOf<RecommendationMode>()
        val suggestionHouseholdIds = mutableListOf<String>()
        val ingredientOptionHouseholdIds = mutableListOf<String>()
        val recipeSearchRequests = mutableListOf<RecipeSearchRequest>()
        var likedRecipes = emptyList<Recipe>()

        override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> {
            recipeHouseholdIds += householdId
            recipeModes += mode
            error?.let { throw it }
            return recipes
        }

        override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> {
            suggestionHouseholdIds += householdId
            error?.let { throw it }
            return suggestions
        }

        override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> {
            ingredientOptionHouseholdIds += householdId
            error?.let { throw it }
            return listOf(
                RecipeIngredientOption(
                    id = "rice-id",
                    name = "Rice",
                    categoryName = "Cereals",
                    remainingAmount = 1.0,
                    unit = "PIECES",
                    expiring = false
                ),
                RecipeIngredientOption(
                    id = "tomato-id",
                    name = "Tomato",
                    categoryName = "Vegetables",
                    remainingAmount = 2.0,
                    unit = "PIECES",
                    expiring = false
                )
            )
        }

        override suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe> {
            recipeSearchRequests += request
            error?.let { throw it }
            return foundRecipes
        }

        override suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe =
            error?.let { throw it } ?: Recipe(
                title = "ИИ-рецепт",
                ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
                steps = listOf("Cook rice"),
                time = "15 minutes",
                calories = 300,
                aiGenerated = true
            )

        override suspend fun getLikedRecipes(householdId: String): List<Recipe> =
            likedRecipes

        override suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> {
            likedRecipes = if (liked) {
                likedRecipes.filterNot { it.localIdentity() == recipe.localIdentity() } + recipe
            } else {
                likedRecipes.filterNot { it.localIdentity() == recipe.localIdentity() }
            }
            return likedRecipes
        }
    }

    private fun recipe(
        title: String = "Rice Bowl",
        time: String = "15 minutes",
        aiGenerated: Boolean = false
    ): Recipe =
        Recipe(
            title = title,
            ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = time,
            calories = 300,
            aiGenerated = aiGenerated
        )
}
