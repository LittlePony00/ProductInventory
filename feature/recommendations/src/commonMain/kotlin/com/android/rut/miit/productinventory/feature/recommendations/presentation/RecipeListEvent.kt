package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.common.UiEvent
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter

sealed class RecipeListEvent : UiEvent {
    data class OnCreate(val householdId: String) : RecipeListEvent()
    data class OnTabSelected(val tab: RecipeListTab) : RecipeListEvent()
    data object OnDiscoverTabClick : RecipeListEvent()
    data object OnLikedTabClick : RecipeListEvent()
    data object OnGenerateClick : RecipeListEvent()
    data object OnGenerateFromCurrentProductsClick : RecipeListEvent()
    data class OnIngredientSelectionChanged(val ingredientId: String) : RecipeListEvent()
    data object OnIngredientDialogDismissed : RecipeListEvent()
    data object OnFindSelectedRecipeClick : RecipeListEvent()
    data object OnRandomRecipeClick : RecipeListEvent()
    data object OnUseSoonClick : RecipeListEvent()
    data class OnQuickFilterChanged(val filter: RecipeQuickFilter) : RecipeListEvent()
    data class OnRecipeLikeClick(val recipe: Recipe) : RecipeListEvent()
    data object OnRetry : RecipeListEvent()
    data class OnRecipeClick(val recipeId: String) : RecipeListEvent()
    data object OnBackClick : RecipeListEvent()
}
