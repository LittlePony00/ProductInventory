package com.android.rut.miit.productinventory.application.mapper

import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDate
import java.util.UUID

class ProductDtoMapperTest {

    @Test
    fun `maps extended product fields to response`() {
        val purchaseDate = LocalDate.of(2026, 5, 14)
        val expirationDate = LocalDate.now().plusDays(10)
        val product = Product(
            id = UUID.randomUUID(),
            name = "Yogurt",
            brand = "Brand",
            barcode = "4601234567890",
            category = ProductCategory.DAIRY,
            quantity = Quantity(4.0, QuantityUnit.PIECES),
            packageQuantity = Quantity(125.0, QuantityUnit.GRAMS),
            ingredientsText = "Milk, cultures",
            imageUrl = "https://cdn.example.test/yogurt.jpg",
            calories = 80.0,
            protein = 4.0,
            fat = 2.5,
            carbs = 9.0,
            purchaseDate = purchaseDate,
            remainingAmount = 2.0,
            lowStockThreshold = 1.0,
            expirationDate = ExpirationDate(expirationDate),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

        val response = product.toResponse()

        assertEquals(product.brand, response.brand)
        assertEquals(product.barcode, response.barcode)
        assertEquals(product.packageQuantity?.value, response.packageAmount)
        assertEquals(product.packageQuantity?.unit, response.packageUnit)
        assertEquals(product.ingredientsText, response.ingredientsText)
        assertEquals(product.imageUrl, response.imageUrl)
        assertEquals(product.calories, response.calories)
        assertEquals(product.protein, response.protein)
        assertEquals(product.fat, response.fat)
        assertEquals(product.carbs, response.carbs)
        assertEquals(purchaseDate, response.purchaseDate)
        assertEquals(2.0, response.remainingAmount)
        assertEquals(1.0, response.lowStockThreshold)
        assertEquals(expirationDate, response.expirationDate)
    }
}
