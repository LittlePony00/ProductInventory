package com.android.rut.miit.productinventory.feature.products.data.mappers

import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class ProductMapperTest {

    @Test
    fun `maps extended response fields to domain product`() {
        val product = ProductResponseDto(
            id = "product-id",
            name = "Milk",
            brand = "Brand",
            barcode = "4601234567890",
            category = "DAIRY",
            quantity = 2.0,
            quantityUnit = "PIECES",
            packageAmount = 950.0,
            packageUnit = "MILLILITERS",
            ingredientsText = "Milk",
            calories = 60.0,
            protein = 3.0,
            fat = 2.5,
            carbs = 4.7,
            purchaseDate = "2026-05-14",
            remainingAmount = 1.5,
            lowStockThreshold = 0.5,
            expirationDate = null,
            expirationStatus = "FRESH",
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        ).toDomain()

        assertEquals("Brand", product.brand)
        assertEquals("4601234567890", product.barcode)
        assertEquals(950.0, product.packageAmount)
        assertEquals(QuantityUnit.MILLILITERS, product.packageUnit)
        assertEquals("Milk", product.ingredientsText)
        assertEquals(60.0, product.calories)
        assertEquals(3.0, product.protein)
        assertEquals(2.5, product.fat)
        assertEquals(4.7, product.carbs)
        assertEquals(LocalDate.parse("2026-05-14"), product.purchaseDate)
        assertEquals(1.5, product.remainingAmount)
        assertEquals(0.5, product.lowStockThreshold)
    }

    @Test
    fun `maps old response schema by defaulting missing remaining amount to original quantity`() {
        val response = Json.decodeFromString<ProductResponseDto>(
            """
            {
              "id": "product-id",
              "name": "Milk",
              "category": "DAIRY",
              "quantity": 2.0,
              "quantityUnit": "PIECES",
              "expirationDate": null,
              "expirationStatus": "FRESH",
              "householdId": "household-id",
              "addedByUserId": "user-id",
              "createdAt": "2026-05-14T00:00:00Z"
            }
            """.trimIndent()
        )

        val product = response.toDomain()

        assertNull(product.brand)
        assertNull(product.packageAmount)
        assertEquals(2.0, product.remainingAmount)
    }
}
