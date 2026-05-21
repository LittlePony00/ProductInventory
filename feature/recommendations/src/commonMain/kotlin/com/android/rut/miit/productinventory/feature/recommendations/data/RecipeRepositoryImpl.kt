package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.recommendations.data.models.FindRecipeRequestDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.GenerateAiRecipeRequestDto

class RecipeRepositoryImpl(
    private val remoteDataSource: RecipeRemoteDataSource,
    private val likedRecipeLocalDataSource: LikedRecipeLocalDataSource
) : RecipeRepository {

    override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> {
        return remoteDataSource.getRecipes(householdId, mode).map { it.toDomain() }
    }

    override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> {
        return remoteDataSource.getRecipeSuggestions(householdId).map { it.toDomain() }
    }

    override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> {
        return remoteDataSource.getIngredientOptions(householdId).map { it.toDomain() }
    }

    override suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe> {
        return remoteDataSource.findRecipes(
            householdId = householdId,
            request = FindRecipeRequestDto(selectedProductIds = request.selectedProductIds)
        ).map { it.toDomain() }
    }

    override suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe {
        return remoteDataSource.generateAiRecipe(
            householdId = householdId,
            request = GenerateAiRecipeRequestDto(
                maxCookingTimeMinutes = request.maxCookingTimeMinutes,
                servings = request.servings,
                extraNotes = request.extraNotes
            )
        ).toDomain()
    }

    override suspend fun getLikedRecipes(householdId: String): List<Recipe> =
        likedRecipeLocalDataSource.getLikedRecipes(householdId)

    override suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> =
        likedRecipeLocalDataSource.setLikedRecipe(householdId, recipe, liked)
}
