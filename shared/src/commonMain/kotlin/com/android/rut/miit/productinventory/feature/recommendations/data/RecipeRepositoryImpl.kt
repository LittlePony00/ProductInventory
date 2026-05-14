package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.data.mappers.toDomain

class RecipeRepositoryImpl(
    private val remoteDataSource: RecipeRemoteDataSource
) : RecipeRepository {

    override suspend fun getRecipes(householdId: String): List<Recipe> {
        return remoteDataSource.getRecipes(householdId).map { it.toDomain() }
    }

    override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> {
        return remoteDataSource.getRecipeSuggestions(householdId).map { it.toDomain() }
    }
}
