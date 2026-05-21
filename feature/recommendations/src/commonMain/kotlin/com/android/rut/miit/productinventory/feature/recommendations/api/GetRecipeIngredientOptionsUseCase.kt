package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption

class GetRecipeIngredientOptionsUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(householdId: String): List<RecipeIngredientOption> =
        repository.getIngredientOptions(householdId)
}
