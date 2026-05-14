package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeRepository
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeResult
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeNotFoundResponse
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

class BarcodeRepositoryImpl(
    private val remoteDataSource: BarcodeRemoteDataSource,
    private val localDataSource: BarcodeLocalDataSource
) : BarcodeRepository {

    override suspend fun lookupBarcode(householdId: String, barcode: String): BarcodeResult {
        return try {
            val response = remoteDataSource.lookupAndAddProduct(householdId, barcode)

            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val productDto = response.body<ProductResponseDto>()
                    val product = productDto.toDomain()
                    localDataSource.saveBarcode(
                        CachedBarcodeProduct(
                            barcode = barcode,
                            name = product.name,
                            category = product.category.name,
                            imageUrl = null
                        )
                    )
                    BarcodeResult.ProductFound(product)
                }
                HttpStatusCode.NotFound -> {
                    try {
                        val body = response.body<BarcodeNotFoundResponse>()
                        if (body.needsManualEntry) {
                            BarcodeResult.NeedsManualEntry(barcode)
                        } else {
                            BarcodeResult.Error(body.message)
                        }
                    } catch (e: Exception) {
                        BarcodeResult.NeedsManualEntry(barcode)
                    }
                }
                else -> {
                    BarcodeResult.Error("Server error: ${response.status}")
                }
            }
        } catch (e: Exception) {
            val cached = localDataSource.getCachedBarcode(barcode)
            if (cached != null) {
                BarcodeResult.NeedsManualEntry(barcode)
            } else {
                BarcodeResult.Error(e.message ?: "Unknown error")
            }
        }
    }
}
