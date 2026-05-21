package com.android.rut.miit.productinventory.feature.recommendations.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.recommendations.api.FindRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetLikedRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeIngredientOptionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.SetRecipeLikedUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val getRecipesUseCase: GetRecipesUseCase,
    private val getRecipeSuggestionsUseCase: GetRecipeSuggestionsUseCase,
    private val getRecipeIngredientOptionsUseCase: GetRecipeIngredientOptionsUseCase,
    private val findRecipesUseCase: FindRecipesUseCase,
    private val getLikedRecipesUseCase: GetLikedRecipesUseCase,
    private val setRecipeLikedUseCase: SetRecipeLikedUseCase
) : SharedViewModel<RecipeListState, RecipeListEvent, RecipeListAction>(
    initialState = RecipeListState.Loading
) {

    private var householdId: String = ""
    private var lastMode = RecommendationMode.CURRENT_PRODUCTS
    private var selectedTab = RecipeListTab.DISCOVER
    private var loadedRecipes: List<Recipe> = emptyList()
    private var likedRecipes: List<Recipe> = emptyList()
    private var likedRecipeIds: Set<String> = emptySet()
    private var quickFilters: Set<RecipeQuickFilter> = emptySet()
    private var ingredientOptions: List<RecipeIngredientOption> = emptyList()
    private var selectedIngredientIds: Set<String> = emptySet()
    private var showIngredientDialog = false
    private var ingredientOptionsLoading = false
    private var ingredientOptionsError: String? = null

    override suspend fun handleEvent(event: RecipeListEvent) {
        when (event) {
            is RecipeListEvent.OnCreate -> {
                householdId = event.householdId
                loadRecipes(RecommendationMode.CURRENT_PRODUCTS)
                loadIngredientOptions()
            }
            is RecipeListEvent.OnTabSelected -> selectTab(event.tab)
            RecipeListEvent.OnDiscoverTabClick -> selectTab(RecipeListTab.DISCOVER)
            RecipeListEvent.OnLikedTabClick -> selectTab(RecipeListTab.LIKED)
            RecipeListEvent.OnGenerateClick,
            RecipeListEvent.OnGenerateFromCurrentProductsClick -> showIngredientSelectionDialog()
            is RecipeListEvent.OnIngredientSelectionChanged -> toggleIngredientSelection(event.ingredientId)
            RecipeListEvent.OnIngredientDialogDismissed -> hideIngredientSelectionDialog()
            RecipeListEvent.OnFindSelectedRecipeClick -> findRecipes(selectedIngredientIds)
            RecipeListEvent.OnRandomRecipeClick -> findRecipes(emptySet())
            is RecipeListEvent.OnUseSoonClick -> loadRecipes(RecommendationMode.USE_SOON)
            is RecipeListEvent.OnQuickFilterChanged -> toggleQuickFilter(event.filter)
            is RecipeListEvent.OnRecipeLikeClick -> toggleRecipeLike(event.recipe)
            is RecipeListEvent.OnRetry -> loadRecipes(lastMode)
            is RecipeListEvent.OnRecipeClick ->
                sendAction(RecipeListAction.OpenRecipeDetail(event.recipeId))
            is RecipeListEvent.OnBackClick ->
                sendAction(RecipeListAction.NavigateBack)
        }
    }

    private fun loadRecipes(mode: RecommendationMode) {
        resetQuickFiltersForModeChange(mode)
        lastMode = mode
        viewModelScope.launch {
            updateState { RecipeListState.Loading }
            runCatching {
                getRecipesUseCase(householdId, mode)
            }
                .onSuccess { recipes ->
                    loadedRecipes = recipes
                    refreshLikedRecipes()
                    applyRecipeListState(mode = mode, generated = true)
                }
                .onFailure { updateState { errorState(it.message, selectedMode = mode) } }
        }
    }

    private fun loadIngredientOptions() {
        viewModelScope.launch {
            ingredientOptionsLoading = true
            ingredientOptionsError = null
            applyIngredientDialogState()
            runCatching { getRecipeIngredientOptionsUseCase(householdId) }
                .onSuccess { options ->
                    ingredientOptions = options
                    selectedIngredientIds = selectedIngredientIds intersect options.map { it.id }.toSet()
                    ingredientOptionsError = null
                }
                .onFailure { error ->
                    ingredientOptionsError = error.message ?: "Не удалось загрузить продукты"
                }
            ingredientOptionsLoading = false
            applyIngredientDialogState()
        }
    }

    private fun showIngredientSelectionDialog() {
        showIngredientDialog = true
        if (ingredientOptions.isEmpty() && !ingredientOptionsLoading) {
            loadIngredientOptions()
        }
        applyIngredientDialogState()
    }

    private fun hideIngredientSelectionDialog() {
        showIngredientDialog = false
        applyIngredientDialogState()
    }

    private fun toggleIngredientSelection(ingredientId: String) {
        selectedIngredientIds = if (ingredientId in selectedIngredientIds) {
            selectedIngredientIds - ingredientId
        } else {
            selectedIngredientIds + ingredientId
        }
        applyIngredientDialogState()
    }

    private fun findRecipes(selectedProductIds: Set<String>) {
        if (selectedProductIds.isEmpty() && selectedIngredientIds.isNotEmpty()) {
            selectedIngredientIds = emptySet()
        }
        showIngredientDialog = false
        resetQuickFiltersForModeChange(RecommendationMode.CURRENT_PRODUCTS)
        lastMode = RecommendationMode.CURRENT_PRODUCTS
        viewModelScope.launch {
            updateState { RecipeListState.Loading }
            runCatching {
                findRecipesUseCase(
                    householdId = householdId,
                    request = RecipeSearchRequest(selectedProductIds = selectedProductIds)
                )
            }
                .onSuccess { recipes ->
                    loadedRecipes = recipes
                    selectedTab = RecipeListTab.DISCOVER
                    refreshLikedRecipes()
                    applyRecipeListState(mode = RecommendationMode.CURRENT_PRODUCTS, generated = true)
                }
                .onFailure { updateState { errorState(it.message, selectedMode = RecommendationMode.CURRENT_PRODUCTS) } }
        }
    }

    private fun resetQuickFiltersForModeChange(mode: RecommendationMode) {
        if (mode != lastMode) {
            quickFilters = emptySet()
        }
    }

    private fun toggleQuickFilter(filter: RecipeQuickFilter) {
        quickFilters = if (filter in quickFilters) quickFilters - filter else quickFilters + filter
        applyRecipeListState(mode = lastMode, generated = true)
    }

    private fun selectTab(tab: RecipeListTab) {
        selectedTab = tab
        applyRecipeListState(mode = lastMode, generated = true)
    }

    private fun toggleRecipeLike(recipe: Recipe) {
        viewModelScope.launch {
            val liked = recipe.localIdentity() !in likedRecipeIds
            runCatching { setRecipeLikedUseCase(householdId, recipe, liked) }
                .onSuccess { recipes ->
                    setLikedRecipes(recipes)
                    applyRecipeListState(mode = lastMode, generated = true)
                }
                .onFailure { error ->
                    updateState { errorState(error.message, selectedMode = lastMode) }
                }
        }
    }

    private suspend fun refreshLikedRecipes() {
        runCatching { getLikedRecipesUseCase(householdId) }
            .onSuccess(::setLikedRecipes)
    }

    private fun setLikedRecipes(recipes: List<Recipe>) {
        likedRecipes = recipes
        likedRecipeIds = recipes.map { it.localIdentity() }.toSet()
    }

    private fun applyRecipeListState(
        mode: RecommendationMode,
        generated: Boolean
    ) {
        val visibleRecipes = when (selectedTab) {
            RecipeListTab.DISCOVER -> loadedRecipes.applyFilters(quickFilters)
            RecipeListTab.LIKED -> likedRecipes
        }
        updateState {
            if (visibleRecipes.isEmpty()) {
                emptyState(mode = mode, generated = generated)
            } else {
                contentState(
                    recipes = visibleRecipes,
                    selectedMode = mode
                )
            }
        }
    }

    private fun List<Recipe>.applyFilters(filters: Set<RecipeQuickFilter>): List<Recipe> =
        filter { recipe ->
            filters.all { filter ->
                when (filter) {
                    RecipeQuickFilter.UNDER_30_MIN ->
                        (recipe.cookingTimeMinutes ?: recipe.time.firstNumber())?.let { it <= 30 } == true
                    RecipeQuickFilter.FEW_MISSING_INGREDIENTS -> recipe.missingIngredients.size <= 1
                    RecipeQuickFilter.AI_GENERATED -> recipe.aiGenerated
                }
            }
        }

    @Suppress("unused")
    private suspend fun loadLegacySuggestions(): List<Recipe> {
        return getRecipeSuggestionsUseCase(householdId)
    }

    private fun applyIngredientDialogState() {
        updateState {
            when (this) {
                is RecipeListState.Content -> copy(
                    likedRecipes = this@RecipeListViewModel.likedRecipes,
                    likedRecipeIds = this@RecipeListViewModel.likedRecipeIds,
                    ingredientOptions = this@RecipeListViewModel.ingredientOptions,
                    selectedIngredientIds = this@RecipeListViewModel.selectedIngredientIds,
                    showIngredientDialog = this@RecipeListViewModel.showIngredientDialog,
                    ingredientOptionsLoading = this@RecipeListViewModel.ingredientOptionsLoading,
                    ingredientOptionsError = this@RecipeListViewModel.ingredientOptionsError
                )
                is RecipeListState.Empty -> copy(
                    likedRecipes = this@RecipeListViewModel.likedRecipes,
                    likedRecipeIds = this@RecipeListViewModel.likedRecipeIds,
                    ingredientOptions = this@RecipeListViewModel.ingredientOptions,
                    selectedIngredientIds = this@RecipeListViewModel.selectedIngredientIds,
                    showIngredientDialog = this@RecipeListViewModel.showIngredientDialog,
                    ingredientOptionsLoading = this@RecipeListViewModel.ingredientOptionsLoading,
                    ingredientOptionsError = this@RecipeListViewModel.ingredientOptionsError
                )
                is RecipeListState.Error -> copy(
                    likedRecipes = this@RecipeListViewModel.likedRecipes,
                    likedRecipeIds = this@RecipeListViewModel.likedRecipeIds,
                    ingredientOptions = this@RecipeListViewModel.ingredientOptions,
                    selectedIngredientIds = this@RecipeListViewModel.selectedIngredientIds,
                    showIngredientDialog = this@RecipeListViewModel.showIngredientDialog,
                    ingredientOptionsLoading = this@RecipeListViewModel.ingredientOptionsLoading,
                    ingredientOptionsError = this@RecipeListViewModel.ingredientOptionsError
                )
                RecipeListState.Loading -> this
            }
        }
    }

    private fun contentState(
        recipes: List<Recipe>,
        selectedMode: RecommendationMode
    ): RecipeListState.Content =
        RecipeListState.Content(
            recipes = recipes,
            selectedTab = selectedTab,
            selectedMode = selectedMode,
            quickFilters = quickFilters,
            likedRecipes = likedRecipes,
            likedRecipeIds = likedRecipeIds,
            ingredientOptions = ingredientOptions,
            selectedIngredientIds = selectedIngredientIds,
            showIngredientDialog = showIngredientDialog,
            ingredientOptionsLoading = ingredientOptionsLoading,
            ingredientOptionsError = ingredientOptionsError
        )

    private fun emptyState(
        mode: RecommendationMode,
        generated: Boolean
    ): RecipeListState.Empty =
        RecipeListState.Empty(
            selectedTab = selectedTab,
            mode = mode,
            generated = generated,
            quickFilters = quickFilters,
            likedRecipes = likedRecipes,
            likedRecipeIds = likedRecipeIds,
            ingredientOptions = ingredientOptions,
            selectedIngredientIds = selectedIngredientIds,
            showIngredientDialog = showIngredientDialog,
            ingredientOptionsLoading = ingredientOptionsLoading,
            ingredientOptionsError = ingredientOptionsError
        )

    private fun errorState(
        message: String?,
        selectedMode: RecommendationMode
    ): RecipeListState.Error =
        RecipeListState.Error(
            message = message,
            selectedTab = selectedTab,
            selectedMode = selectedMode,
            likedRecipes = likedRecipes,
            likedRecipeIds = likedRecipeIds,
            ingredientOptions = ingredientOptions,
            selectedIngredientIds = selectedIngredientIds,
            showIngredientDialog = showIngredientDialog,
            ingredientOptionsLoading = ingredientOptionsLoading,
            ingredientOptionsError = ingredientOptionsError
        )
}

private fun String.firstNumber(): Int? =
    Regex("""\d+""").find(this)?.value?.toIntOrNull()
