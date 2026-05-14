package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.Product

class InMemoryProductLocalDataSource : ProductLocalDataSource {
    private val cache = mutableMapOf<String, List<Product>>()

    override suspend fun getProducts(householdId: String): List<Product> {
        return cache[householdId] ?: emptyList()
    }

    override suspend fun saveProducts(householdId: String, products: List<Product>) {
        cache[householdId] = products
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return cache.values.flatten().firstOrNull { it.name == barcode }
    }

    override suspend fun deleteProduct(id: String) {
        cache.forEach { (key, list) ->
            cache[key] = list.filter { it.id != id }
        }
    }

    override suspend fun saveProduct(product: Product) {
        val current = cache[product.householdId]?.toMutableList() ?: mutableListOf()
        current.removeAll { it.id == product.id }
        current.add(product)
        cache[product.householdId] = current
    }
}
