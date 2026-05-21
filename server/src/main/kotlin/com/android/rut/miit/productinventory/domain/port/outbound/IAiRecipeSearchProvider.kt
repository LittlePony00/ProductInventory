package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult

interface IAiRecipeSearchProvider {
    fun searchWebRecipes(context: RecommendationContext): List<RecipeDiscoveryResult>
}
