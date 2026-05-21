package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.data.models.FindRecipeRequestDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.GenerateAiRecipeRequestDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeIngredientOptionDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

class RecipeRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getRecipes(
        householdId: String,
        mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS
    ): List<RecipeResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/recipes") {
            parameter("mode", mode.name)
            recipeDiscoveryTimeout()
        }.body()
    }

    suspend fun getRecipeSuggestions(householdId: String): List<RecipeResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/recipes/suggestions").body()
    }

    suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOptionDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/recipes/ingredients").body()
    }

    suspend fun findRecipes(
        householdId: String,
        request: FindRecipeRequestDto
    ): List<RecipeResponseDto> {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/recipes/search") {
            setBody(request)
            recipeDiscoveryTimeout()
        }.body()
    }

    suspend fun generateAiRecipe(
        householdId: String,
        request: GenerateAiRecipeRequestDto
    ): RecipeResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/recipes/ai-generated") {
            setBody(request)
            recipeDiscoveryTimeout()
        }.body()
    }

    private fun HttpRequestBuilder.recipeDiscoveryTimeout() {
        timeout {
            requestTimeoutMillis = RECIPE_DISCOVERY_TIMEOUT_MS
            socketTimeoutMillis = RECIPE_DISCOVERY_TIMEOUT_MS
        }
    }

    private companion object {
        const val RECIPE_DISCOVERY_TIMEOUT_MS = 60_000L
    }
}
