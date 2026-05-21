package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode

interface RecipeRepository {
    suspend fun getRecipes(
        householdId: String,
        mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS
    ): List<Recipe>

    suspend fun getRecipeSuggestions(householdId: String): List<Recipe>
    suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption>
    suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe>
    suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe
    suspend fun getLikedRecipes(householdId: String): List<Recipe>
    suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe>
}
