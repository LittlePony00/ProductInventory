package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.LocalDate

interface ProductRepository {
    suspend fun getProducts(householdId: String): List<Product>
    suspend fun getProduct(householdId: String, productId: String): Product
    suspend fun addProduct(
        householdId: String,
        name: String,
        category: ProductCategory,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?
    ): Product

    suspend fun updateProduct(
        householdId: String,
        productId: String,
        name: String?,
        category: ProductCategory?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?
    ): Product

    suspend fun deleteProduct(householdId: String, productId: String)
    suspend fun getExpiringProducts(householdId: String, days: Int = 3): List<Product>
}
