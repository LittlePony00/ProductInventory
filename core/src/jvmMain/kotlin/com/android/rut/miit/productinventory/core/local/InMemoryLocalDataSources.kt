package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

class JvmInMemoryProductLocalDataSource : ProductLocalDataSource {
    private val cache = mutableMapOf<String, List<Product>>()
    override suspend fun getProducts(householdId: String) = cache[householdId] ?: emptyList()
    override suspend fun getProduct(householdId: String, id: String): Product? =
        cache[householdId]?.firstOrNull { it.id == id }
    override suspend fun saveProducts(householdId: String, products: List<Product>) { cache[householdId] = products }
    override suspend fun getProductByBarcode(householdId: String, barcode: String): Product? =
        cache[householdId]?.firstOrNull { it.barcode == barcode }
    override suspend fun remapCategoryId(
        householdId: String,
        oldCategoryId: String,
        newCategoryId: String,
        newCategoryName: String
    ) {
        cache[householdId] = cache[householdId].orEmpty().map {
            if (it.categoryId == oldCategoryId) it.copy(categoryId = newCategoryId, categoryName = newCategoryName) else it
        }
    }
    override suspend fun deleteProduct(id: String) { cache.forEach { (k, v) -> cache[k] = v.filter { it.id != id } } }
    override suspend fun saveProduct(product: Product) {
        val current = cache[product.householdId]?.toMutableList() ?: mutableListOf()
        current.removeAll { it.id == product.id }; current.add(product)
        cache[product.householdId] = current
    }
}

class JvmInMemoryHouseholdLocalDataSource : HouseholdLocalDataSource {
    private var cache: List<Household> = emptyList()
    override suspend fun getHouseholds() = cache
    override suspend fun saveHouseholds(households: List<Household>) { cache = households }
    override suspend fun getHouseholdById(id: String) = cache.firstOrNull { it.id == id }
}

class JvmInMemoryBarcodeLocalDataSource : BarcodeLocalDataSource {
    private val cache = mutableMapOf<String, CachedBarcodeProduct>()
    override suspend fun getCachedBarcode(householdId: String, code: String) =
        cache[cacheKey(householdId, code)] ?: cache[cacheKey("__global__", code)]
    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        cache[cacheKey(product.householdId, product.barcode)] = product
    }
    override suspend fun isBarcodeKnown(householdId: String, code: String) =
        cache.containsKey(cacheKey(householdId, code)) || cache.containsKey(cacheKey("__global__", code))

    private fun cacheKey(householdId: String, barcode: String): String = "$householdId|${barcode.trim()}"
}

class JvmInMemoryCategoryLocalDataSource : CategoryLocalDataSource {
    private val cache = mutableMapOf<String, List<ProductCategoryOption>>()
    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
        cache[householdId].orEmpty().filter { includeArchived || !it.archived }
    override suspend fun saveCategories(householdId: String, categories: List<ProductCategoryOption>) {
        cache[householdId] = categories
    }
    override suspend fun saveCategory(category: ProductCategoryOption) {
        val householdId = category.householdId ?: return
        cache[householdId] = cache[householdId].orEmpty().filterNot { it.id == category.id } + category
    }
    override suspend fun archiveCategory(householdId: String, categoryId: String) {
        cache[householdId] = cache[householdId].orEmpty()
            .map { if (it.id == categoryId) it.copy(archived = true) else it }
    }
    override suspend fun deleteCategory(categoryId: String) {
        cache.replaceAll { _, categories -> categories.filterNot { it.id == categoryId } }
    }
}

class JvmInMemorySyncQueue : SyncQueue {
    private val queue = mutableListOf<PendingSyncAction>()
    override suspend fun addPendingAction(action: PendingSyncAction) { queue.add(action) }
    override suspend fun getPendingActions() = queue.toList()
    override suspend fun updatePendingAction(action: PendingSyncAction) {
        queue.replaceAll { if (it.id == action.id) action else it }
    }
    override suspend fun removePendingAction(id: String) { queue.removeAll { it.id == id } }
    override suspend fun clearAll() { queue.clear() }
}
