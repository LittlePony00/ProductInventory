package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeRequest
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

class BarcodeRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun lookupAndAddProduct(householdId: String, barcode: String): HttpResponse {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/barcode") {
            setBody(BarcodeRequest(barcode))
        }
    }

    suspend fun lookupBarcode(barcode: String): HttpResponse {
        return httpClient.get("${ApiConstants.API_V1}/products/barcode/$barcode")
    }
}
