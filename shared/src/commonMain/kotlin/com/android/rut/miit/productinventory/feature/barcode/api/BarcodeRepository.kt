package com.android.rut.miit.productinventory.feature.barcode.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product

interface BarcodeRepository {
    suspend fun lookupBarcode(householdId: String, barcode: String): BarcodeResult
}

sealed class BarcodeResult {
    data class ProductFound(val product: Product) : BarcodeResult()
    data class NeedsManualEntry(val barcode: String) : BarcodeResult()
    data class Error(val message: String) : BarcodeResult()
}
