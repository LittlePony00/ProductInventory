package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.LocalDate

interface ProductRepository {
    suspend fun getProducts(householdId: String, categoryId: String? = null): List<Product>
    suspend fun getProduct(householdId: String, productId: String): Product
    suspend fun addProduct(
        householdId: String,
        name: String,
        category: ProductCategory,
        categoryId: String? = null,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?,
        brand: String? = null,
        barcode: String? = null,
        packageAmount: Double? = null,
        packageUnit: QuantityUnit? = null,
        ingredientsText: String? = null,
        calories: Double? = null,
        protein: Double? = null,
        fat: Double? = null,
        carbs: Double? = null,
        purchaseDate: LocalDate? = null,
        remainingAmount: Double? = null,
        lowStockThreshold: Double? = null
    ): Product

    suspend fun updateProduct(
        householdId: String,
        productId: String,
        name: String?,
        category: ProductCategory?,
        categoryId: String? = null,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?,
        brand: String? = null,
        barcode: String? = null,
        packageAmount: Double? = null,
        packageUnit: QuantityUnit? = null,
        ingredientsText: String? = null,
        calories: Double? = null,
        protein: Double? = null,
        fat: Double? = null,
        carbs: Double? = null,
        purchaseDate: LocalDate? = null,
        remainingAmount: Double? = null,
        lowStockThreshold: Double? = null
    ): Product

    suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product

    suspend fun deleteProduct(householdId: String, productId: String)
    suspend fun getExpiringProducts(householdId: String, days: Int = 3): List<Product>
    suspend fun suggestProductEnrichment(
        householdId: String,
        name: String?,
        brand: String?,
        barcode: String?,
        ingredientsText: String?
    ): ProductEnrichmentSuggestion
    suspend fun upsertCachedProduct(product: Product)
    suspend fun deleteCachedProduct(productId: String)
}
