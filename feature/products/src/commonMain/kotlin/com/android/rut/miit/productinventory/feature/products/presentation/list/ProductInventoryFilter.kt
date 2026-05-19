package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    EXPIRING_SOON,
    EXPIRED
}

data class ProductListFilters(
    val categoryId: String? = null,
    val inventory: InventoryFilter = InventoryFilter.ALL
)

val Product.isLowStock: Boolean
    get() = lowStockThreshold?.let { remainingAmount <= it } == true

fun List<Product>.applyFilters(filters: ProductListFilters): List<Product> =
    asSequence()
        .filter { product -> filters.categoryId == null || product.matchesCategoryFilter(filters.categoryId) }
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

private fun Product.matchesCategoryFilter(categoryId: String): Boolean =
    this.categoryId == categoryId || ProductCategoryOption.systemDefaults()
        .firstOrNull { it.code == category }
        ?.id == categoryId
