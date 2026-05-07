package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.common.UiAction

sealed class RecipeListAction : UiAction {
    data class OpenRecipeDetail(val recipeId: String) : RecipeListAction()
    data object NavigateBack : RecipeListAction()
}
