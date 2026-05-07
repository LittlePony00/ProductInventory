package com.android.rut.miit.productinventory.feature.recommendations.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import kotlinx.coroutines.launch

class RecipeListViewModel(
    private val getRecipesUseCase: GetRecipesUseCase
) : SharedViewModel<RecipeListState, RecipeListEvent, RecipeListAction>(
    initialState = RecipeListState.Loading
) {

    private var householdId: String = ""

    override suspend fun handleEvent(event: RecipeListEvent) {
        when (event) {
            is RecipeListEvent.OnCreate -> {
                householdId = event.householdId
                loadRecipes()
            }
            is RecipeListEvent.OnRetry -> loadRecipes()
            is RecipeListEvent.OnRecipeClick ->
                sendAction(RecipeListAction.OpenRecipeDetail(event.recipeId))
            is RecipeListEvent.OnBackClick ->
                sendAction(RecipeListAction.NavigateBack)
        }
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            updateState { RecipeListState.Loading }
            runCatching { getRecipesUseCase(householdId) }
                .onSuccess { updateState { RecipeListState.Content(it) } }
                .onFailure { updateState { RecipeListState.Error(it.message) } }
        }
    }
}
