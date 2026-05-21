package com.android.rut.miit.productinventory.feature.recommendations.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode

enum class RecipeListTab {
    DISCOVER,
    LIKED
}

sealed class RecipeListState : UiState {
    data object Loading : RecipeListState()
    data class Content(
        val recipes: List<Recipe>,
        val selectedTab: RecipeListTab = RecipeListTab.DISCOVER,
        val selectedMode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS,
        val quickFilters: Set<RecipeQuickFilter> = emptySet(),
        val likedRecipes: List<Recipe> = emptyList(),
        val likedRecipeIds: Set<String> = emptySet(),
        val ingredientOptions: List<RecipeIngredientOption> = emptyList(),
        val selectedIngredientIds: Set<String> = emptySet(),
        val showIngredientDialog: Boolean = false,
        val ingredientOptionsLoading: Boolean = false,
        val ingredientOptionsError: String? = null
    ) : RecipeListState()
    data class Empty(
        val selectedTab: RecipeListTab = RecipeListTab.DISCOVER,
        val mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS,
        val generated: Boolean = false,
        val quickFilters: Set<RecipeQuickFilter> = emptySet(),
        val likedRecipes: List<Recipe> = emptyList(),
        val likedRecipeIds: Set<String> = emptySet(),
        val ingredientOptions: List<RecipeIngredientOption> = emptyList(),
        val selectedIngredientIds: Set<String> = emptySet(),
        val showIngredientDialog: Boolean = false,
        val ingredientOptionsLoading: Boolean = false,
        val ingredientOptionsError: String? = null
    ) : RecipeListState()
    data class Error(
        val message: String?,
        val selectedTab: RecipeListTab = RecipeListTab.DISCOVER,
        val selectedMode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS,
        val likedRecipes: List<Recipe> = emptyList(),
        val likedRecipeIds: Set<String> = emptySet(),
        val ingredientOptions: List<RecipeIngredientOption> = emptyList(),
        val selectedIngredientIds: Set<String> = emptySet(),
        val showIngredientDialog: Boolean = false,
        val ingredientOptionsLoading: Boolean = false,
        val ingredientOptionsError: String? = null
    ) : RecipeListState()
}
