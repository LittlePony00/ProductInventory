package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals

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
                category = ProductCategory.DAIRY,
                inventory = InventoryFilter.EXPIRING_SOON
            )
        )

        assertEquals(listOf("milk"), result.map { it.id })
    }

    private fun product(
        id: String,
        category: ProductCategory = ProductCategory.OTHER,
        remainingAmount: Double = 1.0,
        lowStockThreshold: Double? = null,
        expirationStatus: ExpirationStatus = ExpirationStatus.FRESH
    ): Product =
        Product(
            id = id,
            name = id,
            category = category,
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
