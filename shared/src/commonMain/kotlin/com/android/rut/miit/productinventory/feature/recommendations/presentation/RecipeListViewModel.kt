package com.android.rut.miit.productinventory.feature.recommendations.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val getRecipesUseCase: GetRecipesUseCase,
    private val getRecipeSuggestionsUseCase: GetRecipeSuggestionsUseCase
) : SharedViewModel<RecipeListState, RecipeListEvent, RecipeListAction>(
    initialState = RecipeListState.Loading
) {

    private var householdId: String = ""
    private var lastLoadMode = RecipeLoadMode.Saved

    override suspend fun handleEvent(event: RecipeListEvent) {
        when (event) {
            is RecipeListEvent.OnCreate -> {
                householdId = event.householdId
                loadRecipes(RecipeLoadMode.Saved)
            }
            is RecipeListEvent.OnGenerateClick -> loadRecipes(RecipeLoadMode.Suggestions)
            is RecipeListEvent.OnRetry -> loadRecipes(lastLoadMode)
            is RecipeListEvent.OnRecipeClick ->
                sendAction(RecipeListAction.OpenRecipeDetail(event.recipeId))
            is RecipeListEvent.OnBackClick ->
                sendAction(RecipeListAction.NavigateBack)
        }
    }

    private fun loadRecipes(mode: RecipeLoadMode) {
        lastLoadMode = mode
        viewModelScope.launch {
            updateState { RecipeListState.Loading }
            runCatching {
                when (mode) {
                    RecipeLoadMode.Saved -> getRecipesUseCase(householdId)
                    RecipeLoadMode.Suggestions -> getRecipeSuggestionsUseCase(householdId)
                }
            }
                .onSuccess { updateState { RecipeListState.Content(it) } }
                .onFailure { updateState { RecipeListState.Error(it.message) } }
        }
    }

    private enum class RecipeLoadMode {
        Saved,
        Suggestions
    }
}
