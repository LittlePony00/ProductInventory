package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

class AddProductViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `prefills barcode draft and submits inventory fields`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingProductRepository()
            val viewModel = AddProductViewModel(AddProductUseCase(repository))
            viewModel.householdId = "household-id"

            viewModel.onEvent(
                AddProductEvent.OnPrefill(
                    barcode = "4601234567890",
                    name = "Milk",
                    brand = "Brand",
                    category = ProductCategory.DAIRY,
                    packageAmount = "950",
                    packageUnit = QuantityUnit.MILLILITERS,
                    ingredientsText = "Milk",
                    calories = "60",
                    protein = "3",
                    fat = "2.5",
                    carbs = "4.7"
                )
            )
            viewModel.onEvent(AddProductEvent.OnQuantityChanged("1"))
            viewModel.onEvent(AddProductEvent.OnRemainingAmountChanged("0.5"))
            viewModel.onEvent(AddProductEvent.OnLowStockThresholdChanged("0.75"))
            viewModel.onEvent(AddProductEvent.OnExpirationDateChanged("2026-05-20"))
            advanceUntilIdle()

            viewModel.onEvent(AddProductEvent.OnSaveClick)
            advanceUntilIdle()

            val request = repository.requests.single()
            assertTrue(viewModel.viewState.value.isBarcodePrefilled)
            assertEquals("4601234567890", request.barcode)
            assertEquals("Milk", request.name)
            assertEquals(ProductCategory.DAIRY, request.category)
            assertEquals(0.5, request.remainingAmount)
            assertEquals(0.75, request.lowStockThreshold)
            assertEquals(950.0, request.packageAmount)
            assertEquals(QuantityUnit.MILLILITERS, request.packageUnit)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RecordingProductRepository : ProductRepository {
        val requests = mutableListOf<Request>()

        override suspend fun addProduct(
            householdId: String,
            name: String,
            category: ProductCategory,
            quantity: Double,
            quantityUnit: QuantityUnit,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product {
            requests += Request(
                householdId = householdId,
                name = name,
                brand = brand,
                barcode = barcode,
                category = category,
                quantity = quantity,
                quantityUnit = quantityUnit,
                expirationDate = expirationDate,
                packageAmount = packageAmount,
                packageUnit = packageUnit,
                remainingAmount = remainingAmount,
                lowStockThreshold = lowStockThreshold
            )
            return Product(
                id = "product-id",
                name = name,
                brand = brand,
                barcode = barcode,
                category = category,
                quantity = quantity,
                quantityUnit = quantityUnit,
                remainingAmount = remainingAmount ?: quantity,
                lowStockThreshold = lowStockThreshold,
                expirationDate = expirationDate,
                expirationStatus = ExpirationStatus.FRESH,
                householdId = householdId,
                addedByUserId = "user-id",
                createdAt = "2026-05-14T00:00:00Z"
            )
        }

        override suspend fun getProducts(householdId: String): List<Product> = emptyList()
        override suspend fun getProduct(householdId: String, productId: String): Product = error("unused")
        override suspend fun updateProduct(
            householdId: String,
            productId: String,
            name: String?,
            category: ProductCategory?,
            quantity: Double?,
            quantityUnit: QuantityUnit?,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product = error("unused")

        override suspend fun deleteProduct(householdId: String, productId: String) = Unit
        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()
    }

    private data class Request(
        val householdId: String,
        val name: String,
        val brand: String?,
        val barcode: String?,
        val category: ProductCategory,
        val quantity: Double,
        val quantityUnit: QuantityUnit,
        val expirationDate: LocalDate?,
        val packageAmount: Double?,
        val packageUnit: QuantityUnit?,
        val remainingAmount: Double?,
        val lowStockThreshold: Double?
    )
}
