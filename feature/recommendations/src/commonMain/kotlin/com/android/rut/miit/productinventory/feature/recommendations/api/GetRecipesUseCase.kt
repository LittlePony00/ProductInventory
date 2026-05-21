package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode

class GetRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(
        householdId: String,
        mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS
    ): List<Recipe> =
        repository.getRecipes(householdId, mode)
}
