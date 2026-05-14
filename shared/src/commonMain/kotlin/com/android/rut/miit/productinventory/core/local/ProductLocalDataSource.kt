package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.Product

interface ProductLocalDataSource {
    suspend fun getProducts(householdId: String): List<Product>
    suspend fun saveProducts(householdId: String, products: List<Product>)
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun deleteProduct(id: String)
    suspend fun saveProduct(product: Product)
}
