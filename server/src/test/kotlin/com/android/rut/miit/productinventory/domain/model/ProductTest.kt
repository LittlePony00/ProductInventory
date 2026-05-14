package com.android.rut.miit.productinventory.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import java.util.UUID

class ProductTest {

    @Test
    fun `defaults remaining amount to original quantity`() {
        val product = product()

        kotlin.test.assertEquals(2.0, product.quantity.value)
        kotlin.test.assertEquals(2.0, product.remainingAmount)
    }

    @Test
    fun `rejects negative inventory and nutrition values`() {
        assertFailsWith<IllegalArgumentException> {
            product(remainingAmount = -1.0)
        }

        assertFailsWith<IllegalArgumentException> {
            product(lowStockThreshold = -1.0)
        }

        assertFailsWith<IllegalArgumentException> {
            product(calories = -1.0)
        }
    }

    private fun product(
        remainingAmount: Double = 2.0,
        lowStockThreshold: Double? = null,
        calories: Double? = null
    ) = Product(
        name = "Milk",
        category = ProductCategory.DAIRY,
        quantity = Quantity(2.0, QuantityUnit.PIECES),
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        calories = calories,
        householdId = UUID.randomUUID(),
        addedByUserId = UUID.randomUUID()
    )
}
