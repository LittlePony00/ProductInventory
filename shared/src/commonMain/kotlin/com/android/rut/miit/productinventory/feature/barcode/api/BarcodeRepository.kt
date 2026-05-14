package com.android.rut.miit.productinventory.feature.barcode.api

import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.products.api.models.Product

interface BarcodeRepository {
    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult
    suspend fun addBarcodeProduct(householdId: String, barcode: String): BarcodeAddProductResult
}

sealed class BarcodeLookupResult {
    data class DraftFound(val draft: BarcodeProductDraft) : BarcodeLookupResult()
    data class NeedsManualEntry(val barcode: String) : BarcodeLookupResult()
    data class Error(val message: String) : BarcodeLookupResult()
}

sealed class BarcodeAddProductResult {
    data class ProductAdded(val product: Product) : BarcodeAddProductResult()
    data class NeedsManualEntry(val barcode: String) : BarcodeAddProductResult()
    data class Error(val message: String) : BarcodeAddProductResult()
}

sealed class BarcodeResult {
    data class ProductFound(val product: Product) : BarcodeResult()
    data class NeedsManualEntry(val barcode: String) : BarcodeResult()
    data class DraftFound(val draft: BarcodeProductDraft) : BarcodeResult()
    data class Error(val message: String) : BarcodeResult()
}
