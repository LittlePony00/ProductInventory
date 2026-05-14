package com.android.rut.miit.productinventory.feature.barcode.data.mappers

import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraftResponseDto
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarcodeMapperTest {

    @Test
    fun `maps barcode product draft response to domain`() {
        val draft = BarcodeProductDraftResponseDto(
            barcode = "4601234567890",
            name = "Milk",
            brand = "Brand",
            packageQuantity = 950.0,
            packageQuantityUnit = "MILLILITERS",
            ingredients = "Milk",
            caloriesKcal = 60.0,
            proteinGrams = 3.0,
            fatGrams = 2.5,
            carbohydratesGrams = 4.7,
            category = "DAIRY",
            source = "OPEN_FOOD_FACTS",
            confidence = 0.82
        ).toDomain()

        assertEquals("4601234567890", draft.barcode)
        assertEquals("Milk", draft.name)
        assertEquals("Brand", draft.brand)
        assertEquals(950.0, draft.packageQuantity)
        assertEquals(QuantityUnit.MILLILITERS, draft.packageQuantityUnit)
        assertEquals("Milk", draft.ingredients)
        assertEquals(60.0, draft.caloriesKcal)
        assertEquals(3.0, draft.proteinGrams)
        assertEquals(2.5, draft.fatGrams)
        assertEquals(4.7, draft.carbohydratesGrams)
        assertEquals(ProductCategory.DAIRY, draft.category)
        assertEquals(BarcodeProductSource.OPEN_FOOD_FACTS, draft.source)
        assertEquals(0.82, draft.confidence)
    }

    @Test
    fun `maps unknown enum values safely`() {
        val draft = BarcodeProductDraftResponseDto(
            barcode = "4601234567890",
            packageQuantityUnit = "UNKNOWN_UNIT",
            category = "UNKNOWN_CATEGORY",
            source = "REMOTE_VENDOR",
            confidence = 0.3
        ).toDomain()

        assertNull(draft.packageQuantityUnit)
        assertNull(draft.category)
        assertEquals(BarcodeProductSource.UNKNOWN, draft.source)
    }
}
