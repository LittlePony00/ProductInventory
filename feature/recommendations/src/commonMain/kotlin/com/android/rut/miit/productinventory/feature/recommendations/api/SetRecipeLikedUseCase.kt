package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe

class SetRecipeLikedUseCase(
    private val repository: RecipeRepository
) {
    suspend operator fun invoke(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> =
        repository.setRecipeLiked(householdId, recipe, liked)
}
