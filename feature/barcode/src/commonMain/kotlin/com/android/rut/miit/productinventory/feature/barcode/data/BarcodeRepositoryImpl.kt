package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeRepository
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraftResponseDto
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeNotFoundResponse
import com.android.rut.miit.productinventory.feature.barcode.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

class BarcodeRepositoryImpl(
    private val remoteDataSource: BarcodeRemoteDataSource,
    private val localDataSource: BarcodeLocalDataSource
) : BarcodeRepository {

    override suspend fun lookupBarcode(householdId: String, barcode: String): BarcodeLookupResult {
        return try {
            val response = remoteDataSource.lookupBarcode(householdId, barcode)

            when (response.status) {
                HttpStatusCode.OK -> {
                    val draft = response.body<BarcodeProductDraftResponseDto>().toDomain()
                    localDataSource.saveBarcode(
                        CachedBarcodeProduct(
                            barcode = barcode,
                            name = draft.name.orEmpty(),
                            category = draft.category?.name,
                            imageUrl = null
                        )
                    )
                    BarcodeLookupResult.DraftFound(draft)
                }
                HttpStatusCode.NotFound -> {
                    response.toManualEntryOrError(barcode)
                }
                else -> BarcodeLookupResult.Error("Server error: ${response.status}")
            }
        } catch (e: Exception) {
            val cached = localDataSource.getCachedBarcode(barcode)
            if (cached != null) {
                BarcodeLookupResult.NeedsManualEntry(barcode)
            } else {
                BarcodeLookupResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun addBarcodeProduct(householdId: String, barcode: String): BarcodeAddProductResult {
        return try {
            val response = remoteDataSource.addBarcodeProduct(householdId, barcode)

            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val product = response.body<ProductResponseDto>().toDomain()
                    localDataSource.saveBarcode(
                        CachedBarcodeProduct(
                            barcode = barcode,
                            name = product.name,
                            category = product.category.name,
                            imageUrl = null
                        )
                    )
                    BarcodeAddProductResult.ProductAdded(product)
                }
                HttpStatusCode.NotFound -> {
                    when (val lookup = response.toManualEntryOrError(barcode)) {
                        is BarcodeLookupResult.NeedsManualEntry ->
                            BarcodeAddProductResult.NeedsManualEntry(lookup.barcode)
                        is BarcodeLookupResult.Error ->
                            BarcodeAddProductResult.Error(lookup.message)
                        is BarcodeLookupResult.DraftFound ->
                            BarcodeAddProductResult.Error("Unexpected barcode draft response")
                    }
                }
                else -> BarcodeAddProductResult.Error("Server error: ${response.status}")
            }
        } catch (e: Exception) {
            BarcodeAddProductResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toManualEntryOrError(
        fallbackBarcode: String
    ): BarcodeLookupResult {
        return try {
            val body = body<BarcodeNotFoundResponse>()
            if (body.needsManualEntry) {
                BarcodeLookupResult.NeedsManualEntry(body.barcode)
            } else {
                BarcodeLookupResult.Error(body.message)
            }
        } catch (e: Exception) {
            BarcodeLookupResult.NeedsManualEntry(fallbackBarcode)
        }
    }
}
