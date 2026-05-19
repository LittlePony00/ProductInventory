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

    override suspend fun getCachedBarcode(householdId: String, code: String): CachedBarcodeProduct? {
        val normalizedBarcode = code.normalizedBarcode()
        return mutex.withLock {
            val cache = readCache()
            cache.barcodes[cacheKey(householdId, normalizedBarcode)]?.toDomain()
                ?: cache.barcodes[cacheKey(LEGACY_HOUSEHOLD_ID, normalizedBarcode)]?.toDomain()
        }
    }

    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        val normalizedBarcode = product.barcode.normalizedBarcode()
        mutex.withLock {
            val cache = readCache()
            writeCache(
                cache.copy(
                    barcodes = cache.barcodes + (
                        cacheKey(product.householdId, normalizedBarcode) to product.copy(barcode = normalizedBarcode).toRecord()
                        )
                )
            )
        }
    }

    override suspend fun isBarcodeKnown(householdId: String, code: String): Boolean {
        val normalizedBarcode = code.normalizedBarcode()
        return mutex.withLock {
            val cache = readCache()
            cache.barcodes.containsKey(cacheKey(householdId, normalizedBarcode)) ||
                cache.barcodes.containsKey(cacheKey(LEGACY_HOUSEHOLD_ID, normalizedBarcode))
        }
    }

    private suspend fun readCache(): BarcodeCache {
        migrateV1IfNeeded()
        val raw = store.read(BARCODES_V2_KEY) ?: return BarcodeCache()
        val decoded = runCatching { json.decodeFromString<BarcodeCache>(raw) }
        return decoded.getOrElse {
            store.remove(BARCODES_V2_KEY)
            BarcodeCache()
        }
    }

    private suspend fun writeCache(cache: BarcodeCache) {
        store.write(BARCODES_V2_KEY, json.encodeToString(cache))
    }

    private suspend fun migrateV1IfNeeded() {
        if (store.read(BARCODES_V2_KEY) != null) return
        val raw = store.read(BARCODES_V1_KEY) ?: return
        val v1Cache = runCatching { json.decodeFromString<BarcodeCacheV1>(raw) }.getOrNull()
        val migrated = v1Cache?.barcodes.orEmpty().mapValues { (barcode, record) ->
            BarcodeRecord(
                householdId = LEGACY_HOUSEHOLD_ID,
                barcode = barcode.normalizedBarcode(),
                name = record.name,
                brand = null,
                category = record.category,
                categoryId = null,
                categoryName = null,
                packageQuantity = null,
                packageQuantityUnit = null,
                ingredients = null,
                imageUrl = record.imageUrl,
                localImagePath = null,
                caloriesKcal = null,
                proteinGrams = null,
                fatGrams = null,
                carbohydratesGrams = null,
                source = "LOCAL_CACHE",
                updatedAt = 0L
            )
        }
        if (migrated.isNotEmpty()) {
            store.write(BARCODES_V2_KEY, json.encodeToString(BarcodeCache(migrated)))
        }
        store.remove(BARCODES_V1_KEY)
    }

    private fun BarcodeRecord.toDomain() = CachedBarcodeProduct(
        householdId = householdId,
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        categoryId = categoryId,
        categoryName = categoryName,
        packageQuantity = packageQuantity,
        packageQuantityUnit = packageQuantityUnit,
        ingredients = ingredients,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
        carbohydratesGrams = carbohydratesGrams,
        source = source,
        updatedAt = updatedAt
    )

    private fun CachedBarcodeProduct.toRecord() = BarcodeRecord(
        householdId = householdId,
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        categoryId = categoryId,
        categoryName = categoryName,
        packageQuantity = packageQuantity,
        packageQuantityUnit = packageQuantityUnit,
        ingredients = ingredients,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
        carbohydratesGrams = carbohydratesGrams,
        source = source,
        updatedAt = updatedAt
    )

    private fun cacheKey(householdId: String, barcode: String): String = "$householdId|$barcode"

    private fun String.normalizedBarcode(): String = trim()

    private companion object {
        const val BARCODES_V1_KEY = "local_barcodes_v1"
        const val BARCODES_V2_KEY = "local_barcodes_v2"
        const val LEGACY_HOUSEHOLD_ID = "__global__"
    }
}

@Serializable
private data class BarcodeCache(
    val barcodes: Map<String, BarcodeRecord> = emptyMap()
)

@Serializable
private data class BarcodeRecord(
    val householdId: String,
    val barcode: String,
    val name: String?,
    val brand: String?,
    val category: String?,
    val categoryId: String?,
    val categoryName: String?,
    val packageQuantity: Double?,
    val packageQuantityUnit: String?,
    val ingredients: String?,
    val imageUrl: String?,
    val localImagePath: String?,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    val carbohydratesGrams: Double?,
    val source: String,
    val updatedAt: Long
)

@Serializable
private data class BarcodeCacheV1(
    val barcodes: Map<String, BarcodeRecordV1> = emptyMap()
)

@Serializable
private data class BarcodeRecordV1(
    val barcode: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)
