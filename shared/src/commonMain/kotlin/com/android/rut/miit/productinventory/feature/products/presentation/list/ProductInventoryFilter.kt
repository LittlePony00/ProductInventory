package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    EXPIRING_SOON,
    EXPIRED
}

data class ProductListFilters(
    val category: ProductCategory? = null,
    val inventory: InventoryFilter = InventoryFilter.ALL
)

val Product.isLowStock: Boolean
    get() = lowStockThreshold?.let { remainingAmount <= it } == true

fun List<Product>.applyFilters(filters: ProductListFilters): List<Product> =
    asSequence()
        .filter { product -> filters.category == null || product.category == filters.category }
        .filter { product ->
            when (filters.inventory) {
                InventoryFilter.ALL -> true
                InventoryFilter.LOW_STOCK -> product.isLowStock
                InventoryFilter.EXPIRING_SOON -> product.expirationStatus == ExpirationStatus.EXPIRING_SOON
                InventoryFilter.EXPIRED -> product.expirationStatus == ExpirationStatus.EXPIRED
            }
        }
        .sortedWith(
            compareByDescending<Product> { it.isLowStock }
                .thenBy { it.expirationDate?.toString() ?: "9999-12-31" }
                .thenBy { it.name.lowercase() }
        )
        .toList()
