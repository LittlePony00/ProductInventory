package com.android.rut.miit.productinventory.feature.recommendations.api

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe

interface RecipeRepository {
    suspend fun getRecipes(householdId: String): List<Recipe>
}
