package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest

interface IRecipeProvider {
    fun findRecipes(request: RecipeGenerationRequest): List<Recipe>
}
