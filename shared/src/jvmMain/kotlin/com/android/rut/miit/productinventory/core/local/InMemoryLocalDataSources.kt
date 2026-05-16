package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.products.api.models.Product

class JvmInMemoryProductLocalDataSource : ProductLocalDataSource {
    private val cache = mutableMapOf<String, List<Product>>()
    override suspend fun getProducts(householdId: String) = cache[householdId] ?: emptyList()
    override suspend fun getProduct(householdId: String, id: String): Product? =
        cache[householdId]?.firstOrNull { it.id == id }
    override suspend fun saveProducts(householdId: String, products: List<Product>) { cache[householdId] = products }
    override suspend fun getProductByBarcode(barcode: String): Product? = null
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
    override suspend fun getCachedBarcode(code: String) = cache[code]
    override suspend fun saveBarcode(product: CachedBarcodeProduct) { cache[product.barcode] = product }
    override suspend fun isBarcodeKnown(code: String) = cache.containsKey(code)
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
