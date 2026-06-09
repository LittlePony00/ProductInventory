package com.android.rut.miit.productinventory.feature.recommendations.presentation

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
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {

    @Test
    fun `create shows idle state without loading recipes or ingredient options`() = runRecipeViewModelTest {
        val repository = RecordingRecipeRepository(recipes = listOf(recipe(title = "Rice Bowl")))
        val viewModel = viewModel(repository)

        viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
        advanceUntilIdle()

        val state = assertIs<RecipeListState.Empty>(viewModel.viewState.value)
        assertEquals(false, state.generated)
        assertEquals(emptyList(), repository.recipeModes)
        assertEquals(0, repository.ingredientOptionsRequests)
    }

    @Test
    fun `ingredient selection sends selected ids to recipe search`() = runRecipeViewModelTest {
        val repository = RecordingRecipeRepository(foundRecipes = listOf(recipe(title = "Selected Bowl")))
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
        assertEquals(false, state.showIngredientDialog)
        assertEquals(listOf(setOf("rice-id")), repository.recipeSearchRequests.map { it.selectedProductIds })
    }

    @Test
    fun `liked tab shows saved recipes`() = runRecipeViewModelTest {
        val recipe = recipe(title = "Favorite Bowl")
        val repository = RecordingRecipeRepository(recipes = listOf(recipe))
        val viewModel = viewModel(repository)

        viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnRecipeLikeClick(recipe))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnLikedTabClick)
        advanceUntilIdle()

        val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
        assertEquals(RecipeListTab.LIKED, state.selectedTab)
        assertEquals(listOf("Favorite Bowl"), state.recipes.map { it.title })
        assertEquals(setOf(recipe.localIdentity()), state.likedRecipeIds)
    }

    @Test
    fun `use soon mode clears quick filters`() = runRecipeViewModelTest {
        val repository = RecordingRecipeRepository(
            recipes = listOf(
                recipe(title = "Fast Salad", time = "15 minutes"),
                recipe(title = "Slow Soup", time = "45 minutes")
            ),
            foundRecipes = listOf(
                recipe(title = "Fast Salad", time = "15 minutes"),
                recipe(title = "Slow Soup", time = "45 minutes")
            )
        )
        val viewModel = viewModel(repository)

        viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnRandomRecipeClick)
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnQuickFilterChanged(RecipeQuickFilter.UNDER_30_MIN))
        advanceUntilIdle()
        assertEquals(listOf("Fast Salad"), assertIs<RecipeListState.Content>(viewModel.viewState.value).recipes.map { it.title })

        viewModel.onEvent(RecipeListEvent.OnUseSoonClick)
        advanceUntilIdle()

        val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
        assertEquals(emptySet(), state.quickFilters)
        assertEquals(RecommendationMode.USE_SOON, state.selectedMode)
        assertEquals(listOf("Fast Salad", "Slow Soup"), state.recipes.map { it.title })
    }

    @Test
    fun `recipe click emits detail action`() = runRecipeViewModelTest {
        val viewModel = viewModel(RecordingRecipeRepository())
        val action = async { viewModel.viewAction.first() }

        viewModel.onEvent(RecipeListEvent.OnRecipeClick("recipe-id"))
        advanceUntilIdle()

        assertEquals(RecipeListAction.OpenRecipeDetail("recipe-id"), action.await())
    }

    @Test
    fun `back click emits navigation action`() = runRecipeViewModelTest {
        val viewModel = viewModel(RecordingRecipeRepository())
        val action = async { viewModel.viewAction.first() }

        viewModel.onEvent(RecipeListEvent.OnBackClick)
        advanceUntilIdle()

        assertEquals(RecipeListAction.NavigateBack, action.await())
    }

    @Test
    fun `large recipe list keeps quick filtering correct`() = runRecipeViewModelTest {
        val recipes = (1..600).map { index ->
            recipe(
                title = "Recipe $index",
                time = if (index % 3 == 0) "15 minutes" else "45 minutes"
            )
        }
        val viewModel = viewModel(RecordingRecipeRepository(foundRecipes = recipes))

        viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnRandomRecipeClick)
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnQuickFilterChanged(RecipeQuickFilter.UNDER_30_MIN))
        advanceUntilIdle()

        val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
        assertEquals(200, state.recipes.size)
        assertEquals(true, state.recipes.all { it.time == "15 minutes" })
    }

    @Test
    fun `large liked list keeps local identities stable`() = runRecipeViewModelTest {
        val likedRecipes = (1..300).map { recipe(title = "Liked Recipe $it") }
        val repository = RecordingRecipeRepository(
            recipes = listOf(recipe(title = "Discover Recipe")),
            initialLikedRecipes = likedRecipes
        )
        val viewModel = viewModel(repository)

        viewModel.onEvent(RecipeListEvent.OnCreate("household-id"))
        advanceUntilIdle()
        viewModel.onEvent(RecipeListEvent.OnLikedTabClick)
        advanceUntilIdle()

        val state = assertIs<RecipeListState.Content>(viewModel.viewState.value)
        assertEquals(RecipeListTab.LIKED, state.selectedTab)
        assertEquals(300, state.recipes.size)
        assertEquals(300, state.likedRecipeIds.size)
    }

    private fun runRecipeViewModelTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
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

    private class RecordingRecipeRepository(
        private val recipes: List<Recipe> = emptyList(),
        private val foundRecipes: List<Recipe> = emptyList(),
        initialLikedRecipes: List<Recipe> = emptyList()
    ) : RecipeRepository {
        val recipeModes = mutableListOf<RecommendationMode>()
        val recipeSearchRequests = mutableListOf<RecipeSearchRequest>()
        var ingredientOptionsRequests = 0
            private set
        private var likedRecipes = initialLikedRecipes

        override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> {
            recipeModes += mode
            return recipes
        }

        override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> = emptyList()

        override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> =
            recordIngredientOptionsRequest()

        private fun recordIngredientOptionsRequest(): List<RecipeIngredientOption> {
            ingredientOptionsRequests += 1
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
            return foundRecipes
        }

        override suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe =
            Recipe(
                id = "AI Recipe",
                title = "AI Recipe",
                ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
                steps = listOf("Cook rice"),
                time = "15 minutes",
                calories = 300,
                aiGenerated = true
            )

        override suspend fun getLikedRecipes(householdId: String): List<Recipe> = likedRecipes

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
            id = title,
            title = title,
            ingredients = listOf(RecipeIngredient(name = "Rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = time,
            calories = 300,
            aiGenerated = aiGenerated
        )
}
