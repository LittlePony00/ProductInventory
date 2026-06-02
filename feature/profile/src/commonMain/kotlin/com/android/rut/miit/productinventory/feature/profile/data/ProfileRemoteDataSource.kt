package com.android.rut.miit.productinventory.feature.profile.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.profile.data.models.FoodPreferencesResponseDto
import com.android.rut.miit.productinventory.feature.profile.data.models.FoodPreferencesOptionsResponseDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UpdateFoodPreferencesRequestDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UpdateProfileRequestDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UserProfileResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ProfileRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getProfile(): UserProfileResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/profile").body()
    }

    suspend fun updateProfile(request: UpdateProfileRequestDto): UserProfileResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/profile") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getFoodPreferences(): FoodPreferencesResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/profile/food-preferences").body()
    }

    suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferencesOptionsResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/profile/food-preferences/options") {
            parameter("householdId", householdId)
        }.body()
    }

    suspend fun updateFoodPreferences(request: UpdateFoodPreferencesRequestDto): FoodPreferencesResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/profile/food-preferences") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
