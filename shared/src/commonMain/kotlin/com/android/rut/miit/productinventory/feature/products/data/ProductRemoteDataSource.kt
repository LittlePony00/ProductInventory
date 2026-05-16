package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.products.data.models.CreateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ConsumeProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionResponseDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateProductRequestDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ProductRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getProducts(householdId: String, categoryId: String?): List<ProductResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/products") {
            categoryId?.let { parameter("categoryId", it) }
        }.body()
    }

    suspend fun getProduct(householdId: String, productId: String): ProductResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/products/$productId").body()
    }

    suspend fun addProduct(householdId: String, request: CreateProductRequestDto): ProductResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products") {
            setBody(request)
        }.body()
    }

    suspend fun updateProduct(householdId: String, productId: String, request: UpdateProductRequestDto): ProductResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/households/$householdId/products/$productId") {
            setBody(request)
        }.body()
    }

    suspend fun consumeProduct(
        householdId: String,
        productId: String,
        request: ConsumeProductRequestDto
    ): ProductResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/$productId/consume") {
            setBody(request)
        }.body()
    }

    suspend fun deleteProduct(householdId: String, productId: String) {
        httpClient.delete("${ApiConstants.API_V1}/households/$householdId/products/$productId")
    }

    suspend fun getExpiringProducts(householdId: String, days: Int): List<ProductResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/products/expiring") {
            parameter("days", days)
        }.body()
    }

    suspend fun suggestProductEnrichment(
        householdId: String,
        request: ProductEnrichmentSuggestionRequestDto
    ): ProductEnrichmentSuggestionResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/enrichment/suggest") {
            setBody(request)
        }.body()
    }
}
