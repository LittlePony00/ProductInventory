package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.products.data.models.CategoryResponseDto
import com.android.rut.miit.productinventory.feature.products.data.models.CreateCategoryRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateCategoryRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.*

class CategoryRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getCategories(householdId: String, includeArchived: Boolean): List<CategoryResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/categories") {
            parameter("includeArchived", includeArchived)
        }.body()
    }

    suspend fun createCategory(householdId: String, request: CreateCategoryRequestDto): CategoryResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/categories") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateCategory(
        householdId: String,
        categoryId: String,
        request: UpdateCategoryRequestDto
    ): CategoryResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/households/$householdId/categories/$categoryId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun archiveCategory(householdId: String, categoryId: String) {
        httpClient.delete("${ApiConstants.API_V1}/households/$householdId/categories/$categoryId")
    }
}
