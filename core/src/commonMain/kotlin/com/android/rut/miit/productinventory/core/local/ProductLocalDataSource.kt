package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.Product

interface ProductLocalDataSource {
    suspend fun getProducts(householdId: String): List<Product>
    suspend fun getProduct(householdId: String, id: String): Product?
    suspend fun saveProducts(householdId: String, products: List<Product>)
    suspend fun getProductByBarcode(householdId: String, barcode: String): Product?
    suspend fun remapCategoryId(householdId: String, oldCategoryId: String, newCategoryId: String, newCategoryName: String)
    suspend fun deleteProduct(id: String)
    suspend fun saveProduct(product: Product)
}
