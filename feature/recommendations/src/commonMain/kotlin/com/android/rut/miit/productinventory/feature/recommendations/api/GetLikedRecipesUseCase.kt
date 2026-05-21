package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe

class GetLikedRecipesUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(householdId: String): List<Recipe> =
        repository.getLikedRecipes(householdId)
}
