package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentProductLocalDataSource(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) : ProductLocalDataSource {

    private val mutex = Mutex()

    override suspend fun getProducts(householdId: String): List<Product> {
        return mutex.withLock {
            readCache().productsByHousehold[householdId].orEmpty().map { it.toDomain() }
        }
    }

    override suspend fun saveProducts(householdId: String, products: List<Product>) {
        mutex.withLock {
            val cache = readCache()
            writeCache(
                cache.copy(
                    productsByHousehold = cache.productsByHousehold + (householdId to products.map { it.toRecord() })
                )
            )
        }
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return mutex.withLock {
            readCache()
                .productsByHousehold
                .values
                .asSequence()
                .flatten()
                .firstOrNull { it.barcode == barcode }
                ?.toDomain()
        }
    }

    override suspend fun deleteProduct(id: String) {
        mutex.withLock {
            val cache = readCache()
            writeCache(
                cache.copy(
                    productsByHousehold = cache.productsByHousehold.mapValues { (_, products) ->
                        products.filterNot { it.id == id }
                    }
                )
            )
        }
    }

    override suspend fun saveProduct(product: Product) {
        mutex.withLock {
            val cache = readCache()
            val products = cache.productsByHousehold[product.householdId].orEmpty()
            writeCache(
                cache.copy(
                    productsByHousehold = cache.productsByHousehold + (
                        product.householdId to (products.filterNot { it.id == product.id } + product.toRecord())
                        )
                )
            )
        }
    }

    private suspend fun readCache(): ProductCache {
        val raw = store.read(PRODUCTS_KEY) ?: return ProductCache()
        val decoded = runCatching { json.decodeFromString<ProductCache>(raw) }
        return decoded.getOrElse {
            store.remove(PRODUCTS_KEY)
            ProductCache()
        }
    }

    private suspend fun writeCache(cache: ProductCache) {
        store.write(PRODUCTS_KEY, json.encodeToString(cache))
    }

    private fun ProductRecord.toDomain() = Product(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        category = enumValueOrDefault(category, ProductCategory.OTHER),
        categoryId = categoryId,
        categoryName = categoryName,
        quantity = quantity,
        quantityUnit = enumValueOrDefault(quantityUnit, QuantityUnit.PIECES),
        packageAmount = packageAmount,
        packageUnit = packageUnit?.let { enumValueOrDefault(it, QuantityUnit.PIECES) },
        ingredientsText = ingredientsText,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        purchaseDate = purchaseDate?.toLocalDateOrNull(),
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        expirationDate = expirationDate?.toLocalDateOrNull(),
        expirationStatus = enumValueOrDefault(expirationStatus, ExpirationStatus.UNKNOWN),
        householdId = householdId,
        addedByUserId = addedByUserId,
        createdAt = createdAt
    )

    private fun Product.toRecord() = ProductRecord(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        category = category.name,
        categoryId = categoryId,
        categoryName = categoryName,
        quantity = quantity,
        quantityUnit = quantityUnit.name,
        packageAmount = packageAmount,
        packageUnit = packageUnit?.name,
        ingredientsText = ingredientsText,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        purchaseDate = purchaseDate?.toString(),
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        expirationDate = expirationDate?.toString(),
        expirationStatus = expirationStatus.name,
        householdId = householdId,
        addedByUserId = addedByUserId,
        createdAt = createdAt
    )

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private companion object {
        const val PRODUCTS_KEY = "local_products_v1"
    }
}

@Serializable
private data class ProductCache(
    val productsByHousehold: Map<String, List<ProductRecord>> = emptyMap()
)

@Serializable
private data class ProductRecord(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double,
    val lowStockThreshold: Double? = null,
    val expirationDate: String? = null,
    val expirationStatus: String,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String
)
