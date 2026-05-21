package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredientOption
import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import java.util.UUID

interface IRecommendationService {
    fun getRecipes(
        userId: UUID,
        householdId: UUID,
        mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS
    ): List<RecipeRecommendation>

    fun getIngredientOptions(
        userId: UUID,
        householdId: UUID
    ): List<RecipeIngredientOption>

    fun findRecipes(
        userId: UUID,
        householdId: UUID,
        request: RecipeSearchRequest
    ): List<RecipeRecommendation>

    fun generateAiRecipe(
        userId: UUID,
        householdId: UUID,
        request: AiRecipeGenerationRequest
    ): RecipeRecommendation
}
