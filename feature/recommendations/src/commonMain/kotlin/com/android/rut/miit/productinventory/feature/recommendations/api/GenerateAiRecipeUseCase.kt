package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe

class GenerateAiRecipeUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(
        householdId: String,
        request: AiRecipeGenerationRequest = AiRecipeGenerationRequest()
    ): Recipe =
        repository.generateAiRecipe(householdId, request)
}
