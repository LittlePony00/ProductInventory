package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class RecipeRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getRecipes(householdId: String): List<RecipeResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/recipes").body()
    }
}
