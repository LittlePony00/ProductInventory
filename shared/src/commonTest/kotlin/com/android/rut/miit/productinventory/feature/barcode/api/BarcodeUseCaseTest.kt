package com.android.rut.miit.productinventory.feature.barcode.api

import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductSource
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.datetime.LocalDate

class BarcodeUseCaseTest {

    @Test
    fun `lookup barcode delegates to lookup repository path`() = kotlinx.coroutines.test.runTest {
        val repository = FakeBarcodeRepository(
            lookupResult = BarcodeLookupResult.DraftFound(draft()),
            addResult = BarcodeAddProductResult.ProductAdded(product())
        )

        val result = LookupBarcodeUseCase(repository)("4601234567890")

        assertIs<BarcodeLookupResult.DraftFound>(result)
        assertEquals("lookup:4601234567890", repository.calls.single())
    }

    @Test
    fun `add barcode product delegates to add repository path`() = kotlinx.coroutines.test.runTest {
        val repository = FakeBarcodeRepository(
            lookupResult = BarcodeLookupResult.NeedsManualEntry("4601234567890"),
            addResult = BarcodeAddProductResult.ProductAdded(product())
        )

        val result = AddBarcodeProductUseCase(repository)("household-id", "4601234567890")

        assertIs<BarcodeAddProductResult.ProductAdded>(result)
        assertEquals("add:household-id:4601234567890", repository.calls.single())
    }

    private class FakeBarcodeRepository(
        private val lookupResult: BarcodeLookupResult,
        private val addResult: BarcodeAddProductResult
    ) : BarcodeRepository {
        val calls = mutableListOf<String>()

        override suspend fun lookupBarcode(barcode: String): BarcodeLookupResult {
            calls += "lookup:$barcode"
            return lookupResult
        }

        override suspend fun addBarcodeProduct(
            householdId: String,
            barcode: String
        ): BarcodeAddProductResult {
            calls += "add:$householdId:$barcode"
            return addResult
        }
    }

    private fun draft(): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = "4601234567890",
            name = "Milk",
            brand = "Brand",
            packageQuantity = 950.0,
            packageQuantityUnit = QuantityUnit.MILLILITERS,
            ingredients = "Milk",
            caloriesKcal = 60.0,
            proteinGrams = 3.0,
            fatGrams = 2.5,
            carbohydratesGrams = 4.7,
            category = ProductCategory.DAIRY,
            source = BarcodeProductSource.OPEN_FOOD_FACTS,
            confidence = 0.9
        )

    private fun product(): Product =
        Product(
            id = "product-id",
            name = "Milk",
            brand = "Brand",
            barcode = "4601234567890",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-14"),
            expirationStatus = ExpirationStatus.FRESH,
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        )
}
