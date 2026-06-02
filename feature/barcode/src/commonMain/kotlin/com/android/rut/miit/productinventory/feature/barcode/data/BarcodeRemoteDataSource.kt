package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*

class BarcodeRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun addBarcodeProduct(householdId: String, barcode: String): HttpResponse {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/products/barcode") {
            contentType(ContentType.Application.Json)
            setBody(BarcodeRequest(barcode))
        }
    }

    suspend fun lookupBarcode(householdId: String, barcode: String): HttpResponse {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/barcodes/$barcode")
    }
}
