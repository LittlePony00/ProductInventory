package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.common.UiEvent

sealed class RecipeListEvent : UiEvent {
    data class OnCreate(val householdId: String) : RecipeListEvent()
    data object OnRetry : RecipeListEvent()
    data class OnRecipeClick(val recipeId: String) : RecipeListEvent()
    data object OnBackClick : RecipeListEvent()
}
