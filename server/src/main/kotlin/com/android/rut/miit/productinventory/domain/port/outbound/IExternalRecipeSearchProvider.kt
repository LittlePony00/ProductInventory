package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult

interface IExternalRecipeSearchProvider {
    fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult>
    fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> = emptyList()
}
