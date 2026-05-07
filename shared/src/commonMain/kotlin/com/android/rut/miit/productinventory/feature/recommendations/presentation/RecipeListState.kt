package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe

sealed class RecipeListState : UiState {
    data object Loading : RecipeListState()
    data class Content(val recipes: List<Recipe>) : RecipeListState()
    data class Error(val message: String?) : RecipeListState()
}
