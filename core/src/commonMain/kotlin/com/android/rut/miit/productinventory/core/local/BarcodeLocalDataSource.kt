package com.android.rut.miit.productinventory.core.local

data class CachedBarcodeProduct(
    val barcode: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)

interface BarcodeLocalDataSource {
    suspend fun getCachedBarcode(code: String): CachedBarcodeProduct?
    suspend fun saveBarcode(product: CachedBarcodeProduct)
    suspend fun isBarcodeKnown(code: String): Boolean
}
