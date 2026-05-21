package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest

class FindRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(
        householdId: String,
        request: RecipeSearchRequest = RecipeSearchRequest()
    ): List<Recipe> =
        repository.findRecipes(householdId, request)
}
