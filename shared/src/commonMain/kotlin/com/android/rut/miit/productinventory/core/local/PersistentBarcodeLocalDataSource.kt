package com.android.rut.miit.productinventory.core.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentBarcodeLocalDataSource(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) : BarcodeLocalDataSource {

    private val mutex = Mutex()

    override suspend fun getCachedBarcode(code: String): CachedBarcodeProduct? {
        return mutex.withLock { readCache().barcodes[code]?.toDomain() }
    }

    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        mutex.withLock {
            val cache = readCache()
            writeCache(cache.copy(barcodes = cache.barcodes + (product.barcode to product.toRecord())))
        }
    }

    override suspend fun isBarcodeKnown(code: String): Boolean {
        return mutex.withLock { readCache().barcodes.containsKey(code) }
    }

    private suspend fun readCache(): BarcodeCache {
        val raw = store.read(BARCODES_KEY) ?: return BarcodeCache()
        val decoded = runCatching { json.decodeFromString<BarcodeCache>(raw) }
        return decoded.getOrElse {
            store.remove(BARCODES_KEY)
            BarcodeCache()
        }
    }

    private suspend fun writeCache(cache: BarcodeCache) {
        store.write(BARCODES_KEY, json.encodeToString(cache))
    }

    private fun BarcodeRecord.toDomain() = CachedBarcodeProduct(
        barcode = barcode,
        name = name,
        category = category,
        imageUrl = imageUrl
    )

    private fun CachedBarcodeProduct.toRecord() = BarcodeRecord(
        barcode = barcode,
        name = name,
        category = category,
        imageUrl = imageUrl
    )

    private companion object {
        const val BARCODES_KEY = "local_barcodes_v1"
    }
}

@Serializable
private data class BarcodeCache(
    val barcodes: Map<String, BarcodeRecord> = emptyMap()
)

@Serializable
private data class BarcodeRecord(
    val barcode: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)
