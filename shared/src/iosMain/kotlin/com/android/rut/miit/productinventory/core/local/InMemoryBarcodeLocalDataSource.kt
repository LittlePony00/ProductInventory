package com.android.rut.miit.productinventory.core.local

class InMemoryBarcodeLocalDataSource : BarcodeLocalDataSource {
    private val cache = mutableMapOf<String, CachedBarcodeProduct>()

    override suspend fun getCachedBarcode(code: String): CachedBarcodeProduct? = cache[code]

    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        cache[product.barcode] = product
    }

    override suspend fun isBarcodeKnown(code: String): Boolean = cache.containsKey(code)
}
