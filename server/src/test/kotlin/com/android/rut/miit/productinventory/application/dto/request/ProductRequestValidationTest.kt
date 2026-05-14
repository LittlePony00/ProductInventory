package com.android.rut.miit.productinventory.application.dto.request

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import jakarta.validation.Validation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductRequestValidationTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `create request accepts omitted optional inventory details`() {
        val violations = validator.validate(
            CreateProductRequest(
                name = "Milk",
                category = ProductCategory.DAIRY,
                quantity = 2.0,
                quantityUnit = QuantityUnit.PIECES
            )
        )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `create request rejects negative optional inventory details`() {
        val violations = validator.validate(
            CreateProductRequest(
                name = "Milk",
                category = ProductCategory.DAIRY,
                quantity = 2.0,
                quantityUnit = QuantityUnit.PIECES,
                packageAmount = -1.0,
                calories = -1.0,
                protein = -1.0,
                fat = -1.0,
                carbs = -1.0,
                remainingAmount = -1.0,
                lowStockThreshold = -1.0
            )
        )

        assertEquals(
            setOf("packageAmount", "calories", "protein", "fat", "carbs", "remainingAmount", "lowStockThreshold"),
            violations.map { it.propertyPath.toString() }.toSet()
        )
    }

    @Test
    fun `update request accepts all fields omitted`() {
        val violations = validator.validate(UpdateProductRequest())

        assertTrue(violations.isEmpty())
    }

    @Test
    fun `update request rejects negative optional inventory details`() {
        val violations = validator.validate(
            UpdateProductRequest(
                quantity = -1.0,
                packageAmount = -1.0,
                calories = -1.0,
                protein = -1.0,
                fat = -1.0,
                carbs = -1.0,
                remainingAmount = -1.0,
                lowStockThreshold = -1.0
            )
        )

        assertEquals(
            setOf(
                "quantity",
                "packageAmount",
                "calories",
                "protein",
                "fat",
                "carbs",
                "remainingAmount",
                "lowStockThreshold"
            ),
            violations.map { it.propertyPath.toString() }.toSet()
        )
    }
}
