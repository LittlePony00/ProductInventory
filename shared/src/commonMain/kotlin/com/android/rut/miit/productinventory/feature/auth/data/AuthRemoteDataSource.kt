package com.android.rut.miit.productinventory.feature.auth.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.auth.data.models.AuthResponseDto
import com.android.rut.miit.productinventory.feature.auth.data.models.LoginRequestDto
import com.android.rut.miit.productinventory.feature.auth.data.models.RegisterRequestDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class AuthRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun register(email: String, password: String, name: String): AuthResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(email, password, name))
        }.body()
    }

    suspend fun login(email: String, password: String): AuthResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(email, password))
        }.body()
    }
}
