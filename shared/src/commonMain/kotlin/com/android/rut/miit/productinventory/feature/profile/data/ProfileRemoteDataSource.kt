package com.android.rut.miit.productinventory.feature.profile.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.profile.data.models.UpdateProfileRequestDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UserProfileResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ProfileRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getProfile(): UserProfileResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/profile").body()
    }

    suspend fun updateProfile(request: UpdateProfileRequestDto): UserProfileResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/profile") {
            setBody(request)
        }.body()
    }
}
