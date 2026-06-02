package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.products.data.models.CreateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ConsumeProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionResponseDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.UploadProductImageRequestDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun updateProduct(householdId: String, productId: String, request: UpdateProductRequestDto): ProductResponseDto {
        return httpClient.put("${ApiConstants.API_V1}/households/$householdId/products/$productId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun uploadProductImage(
        householdId: String,
        productId: String,
        image: ProductImageFileContent
    ): ProductResponseDto =
        runCatching { uploadProductImageMultipart(householdId, productId, image) }
            .getOrElse { uploadProductImageBytes(householdId, productId, image) }

    private suspend fun uploadProductImageMultipart(
        householdId: String,
        productId: String,
        image: ProductImageFileContent
    ): ProductResponseDto =
        httpClient.submitFormWithBinaryData(
            url = "${ApiConstants.API_V1}/households/$householdId/products/$productId/image",
            formData = formData {
                append(
                    key = "file",
                    value = image.bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, image.contentType)
                        append(
                            HttpHeaders.ContentDisposition,
                            "form-data; name=\"file\"; filename=\"${image.fileName.replace("\"", "")}\""
                        )
                    }
                )
            }
        ).body()

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun uploadProductImageBytes(
        householdId: String,
        productId: String,
        image: ProductImageFileContent
    ): ProductResponseDto =
        httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/$productId/image-bytes") {
            contentType(ContentType.Application.Json)
            setBody(
                UploadProductImageRequestDto(
                    fileName = image.fileName,
                    contentType = image.contentType,
                    bytesBase64 = Base64.Default.encode(image.bytes)
                )
            )
        }.body()

    suspend fun downloadProductImage(imageUrl: String): ByteArray =
        httpClient.get(imageUrl).body()

    suspend fun deleteProductImage(householdId: String, productId: String): ProductResponseDto {
        return httpClient.delete("${ApiConstants.API_V1}/households/$householdId/products/$productId/image").body()
    }

    suspend fun consumeProduct(
        householdId: String,
        productId: String,
        request: ConsumeProductRequestDto
    ): ProductResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/$productId/consume") {
            contentType(ContentType.Application.Json)
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
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
