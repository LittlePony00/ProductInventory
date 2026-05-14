package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.ProductEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDate
import java.util.UUID

class ProductEntityMapperTest {

    @Test
    fun `maps extended product fields to entity`() {
        val product = product()

        val entity = product.toEntity()

        assertEquals(product.brand, entity.brand)
        assertEquals(product.barcode, entity.barcode)
        assertEquals(product.packageQuantity?.value, entity.packageAmount)
        assertEquals(product.packageQuantity?.unit?.name, entity.packageUnit)
        assertEquals(product.ingredientsText, entity.ingredientsText)
        assertEquals(product.calories, entity.calories)
        assertEquals(product.protein, entity.protein)
        assertEquals(product.fat, entity.fat)
        assertEquals(product.carbs, entity.carbs)
        assertEquals(product.purchaseDate, entity.purchaseDate)
        assertEquals(product.remainingAmount, entity.remainingAmount)
        assertEquals(product.lowStockThreshold, entity.lowStockThreshold)
    }

    @Test
    fun `maps extended product fields to domain`() {
        val entity = ProductEntity(
            id = UUID.randomUUID(),
            name = "Cereal",
            brand = "Brand",
            barcode = "1234567890123",
            category = ProductCategory.CEREALS.name,
            quantity = 3.0,
            quantityUnit = QuantityUnit.PIECES.name,
            packageAmount = 450.0,
            packageUnit = QuantityUnit.GRAMS.name,
            ingredientsText = "Oats",
            calories = 340.0,
            protein = 13.0,
            fat = 6.0,
            carbs = 60.0,
            purchaseDate = LocalDate.of(2026, 5, 14),
            remainingAmount = 1.5,
            lowStockThreshold = 1.0,
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

        val product = entity.toDomain()

        assertEquals(entity.brand, product.brand)
        assertEquals(entity.barcode, product.barcode)
        assertEquals(entity.packageAmount, product.packageQuantity?.value)
        assertEquals(QuantityUnit.valueOf(requireNotNull(entity.packageUnit)), product.packageQuantity?.unit)
        assertEquals(entity.ingredientsText, product.ingredientsText)
        assertEquals(entity.calories, product.calories)
        assertEquals(entity.protein, product.protein)
        assertEquals(entity.fat, product.fat)
        assertEquals(entity.carbs, product.carbs)
        assertEquals(entity.purchaseDate, product.purchaseDate)
        assertEquals(entity.remainingAmount, product.remainingAmount)
        assertEquals(entity.lowStockThreshold, product.lowStockThreshold)
    }

    @Test
    fun `uses product quantity unit when package unit is omitted`() {
        val entity = ProductEntity(
            name = "Juice",
            category = ProductCategory.BEVERAGES.name,
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES.name,
            packageAmount = 1_000.0,
            packageUnit = null,
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

        val product = entity.toDomain()

        assertEquals(1_000.0, product.packageQuantity?.value)
        assertEquals(QuantityUnit.PIECES, product.packageQuantity?.unit)
    }

    private fun product() = Product(
        id = UUID.randomUUID(),
        name = "Cereal",
        brand = "Brand",
        barcode = "1234567890123",
        category = ProductCategory.CEREALS,
        quantity = Quantity(3.0, QuantityUnit.PIECES),
        packageQuantity = Quantity(450.0, QuantityUnit.GRAMS),
        ingredientsText = "Oats",
        calories = 340.0,
        protein = 13.0,
        fat = 6.0,
        carbs = 60.0,
        purchaseDate = LocalDate.of(2026, 5, 14),
        remainingAmount = 1.5,
        lowStockThreshold = 1.0,
        householdId = UUID.randomUUID(),
        addedByUserId = UUID.randomUUID()
    )
}
