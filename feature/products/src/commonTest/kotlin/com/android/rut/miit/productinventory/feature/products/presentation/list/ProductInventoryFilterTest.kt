package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductInventoryFilterTest {

    @Test
    fun `filters low stock products`() {
        val products = listOf(
            product(id = "normal", remainingAmount = 5.0, lowStockThreshold = 2.0),
            product(id = "low", remainingAmount = 1.0, lowStockThreshold = 2.0)
        )

        val result = products.applyFilters(ProductListFilters(inventory = InventoryFilter.LOW_STOCK))

        assertEquals(listOf("low"), result.map { it.id })
    }

    @Test
    fun `filters by category and expiration status`() {
        val products = listOf(
            product(id = "milk", category = ProductCategory.DAIRY, expirationStatus = ExpirationStatus.EXPIRING_SOON),
            product(id = "juice", category = ProductCategory.BEVERAGES, expirationStatus = ExpirationStatus.EXPIRING_SOON),
            product(id = "old", category = ProductCategory.DAIRY, expirationStatus = ExpirationStatus.EXPIRED)
        )

        val result = products.applyFilters(
            ProductListFilters(
                categoryId = "dairy",
                inventory = InventoryFilter.EXPIRING_SOON
            )
        )

        assertEquals(listOf("milk"), result.map { it.id })
    }

    @Test
    fun `load style filters low stock products from large inventory`() {
        val products = (1..5_000).map { index ->
            product(
                id = "product-$index",
                remainingAmount = if (index % 5 == 0) 1.0 else 10.0,
                lowStockThreshold = 2.0
            )
        }

        val result = products.applyFilters(ProductListFilters(inventory = InventoryFilter.LOW_STOCK))

        assertEquals(1_000, result.size)
        assertTrue(result.all { it.isLowStock })
        assertEquals("product-10", result.first().id)
    }

    @Test
    fun `load style filters category and expiration from large inventory`() {
        val products = (1..4_000).map { index ->
            product(
                id = "product-$index",
                category = if (index % 2 == 0) ProductCategory.DAIRY else ProductCategory.BEVERAGES,
                expirationStatus = if (index % 4 == 0) ExpirationStatus.EXPIRING_SOON else ExpirationStatus.FRESH
            )
        }

        val result = products.applyFilters(
            ProductListFilters(
                categoryId = ProductCategoryOption.DAIRY_SYSTEM_ID,
                inventory = InventoryFilter.EXPIRING_SOON
            )
        )

        assertEquals(1_000, result.size)
        assertTrue(result.all { it.category == ProductCategory.DAIRY })
        assertTrue(result.all { it.expirationStatus == ExpirationStatus.EXPIRING_SOON })
    }

    @Test
    fun `load style all filter keeps deterministic ordering for large inventory`() {
        val products = (1..2_000).map { index ->
            product(
                id = "product-$index",
                name = "Product ${(2_001 - index).toString().padStart(4, '0')}",
                remainingAmount = if (index == 1_500) 1.0 else 10.0,
                lowStockThreshold = 2.0
            )
        }

        val result = products.applyFilters(ProductListFilters())

        assertEquals(2_000, result.size)
        assertEquals("product-1500", result.first().id)
        assertEquals("Product 2000", result.last().name)
    }

    private fun product(
        id: String,
        name: String = id,
        category: ProductCategory = ProductCategory.OTHER,
        categoryId: String? = category.name.lowercase(),
        remainingAmount: Double = 1.0,
        lowStockThreshold: Double? = null,
        expirationStatus: ExpirationStatus = ExpirationStatus.FRESH
    ): Product =
        Product(
            id = id,
            name = name,
            category = category,
            categoryId = categoryId,
            quantity = 10.0,
            quantityUnit = QuantityUnit.PIECES,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = null,
            expirationStatus = expirationStatus,
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        )
}
