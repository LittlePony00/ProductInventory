package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.SuggestProductEnrichmentUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            val viewModel = AddProductViewModel(
                AddProductUseCase(repository),
                UpdateProductUseCase(repository),
                GetProductUseCase(repository),
                GetProductCategoriesUseCase(FakeCategoryRepository()),
                CreateProductCategoryUseCase(FakeCategoryRepository()),
                SuggestProductEnrichmentUseCase(repository)
            )
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `applies product enrichment suggestion without overwriting entered name`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingProductRepository()
            val viewModel = AddProductViewModel(
                AddProductUseCase(repository),
                UpdateProductUseCase(repository),
                GetProductUseCase(repository),
                GetProductCategoriesUseCase(FakeCategoryRepository()),
                CreateProductCategoryUseCase(FakeCategoryRepository()),
                SuggestProductEnrichmentUseCase(repository)
            )
            viewModel.householdId = "household-id"
            viewModel.onEvent(AddProductEvent.OnCreate("household-id"))
            viewModel.onEvent(AddProductEvent.OnNameChanged("Milk"))
            advanceUntilIdle()

            viewModel.onEvent(AddProductEvent.OnSuggestProductClick)
            advanceUntilIdle()

            val state = viewModel.viewState.value
            assertEquals("Milk", state.name)
            assertEquals(ProductCategory.DAIRY, state.category)
            assertEquals("system-dairy", state.categoryId)
            assertEquals("60", state.calories)
            assertTrue(state.suggestionMessage?.contains("Молочные") == true)
            assertFalse(state.suggestionMessage.orEmpty().contains("%"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads existing product and submits update`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = RecordingProductRepository()
            val viewModel = AddProductViewModel(
                AddProductUseCase(repository),
                UpdateProductUseCase(repository),
                GetProductUseCase(repository),
                GetProductCategoriesUseCase(FakeCategoryRepository()),
                CreateProductCategoryUseCase(FakeCategoryRepository()),
                SuggestProductEnrichmentUseCase(repository)
            )

            viewModel.onEvent(AddProductEvent.OnCreate("household-id"))
            viewModel.onEvent(AddProductEvent.OnLoadProduct("product-id"))
            advanceUntilIdle()
            val loadedState = viewModel.viewState.value
            assertEquals("Milk", loadedState.name)
            assertEquals("Brand", loadedState.brand)
            assertEquals("4601234567890", loadedState.barcode)
            assertEquals(ProductCategory.DAIRY, loadedState.category)
            assertEquals("system-dairy", loadedState.categoryId)
            assertEquals("1", loadedState.quantity)
            assertEquals("0.5", loadedState.remainingAmount)
            assertEquals("2026-05-20", loadedState.expirationDate)

            viewModel.onEvent(AddProductEvent.OnNameChanged("Updated milk"))
            viewModel.onEvent(AddProductEvent.OnSaveClick)
            advanceUntilIdle()

            val request = repository.updateRequests.single()
            assertEquals("product-id", request.productId)
            assertEquals("Updated milk", request.name)
            assertEquals(1.0, request.quantity)
            assertEquals(0.5, request.remainingAmount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RecordingProductRepository : ProductRepository {
        val requests = mutableListOf<Request>()
        val updateRequests = mutableListOf<UpdateRequest>()

        override suspend fun addProduct(
            householdId: String,
            name: String,
            category: ProductCategory,
            categoryId: String?,
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
                categoryId = categoryId,
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
                categoryId = categoryId,
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

        override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> = emptyList()
        override suspend fun getProduct(householdId: String, productId: String): Product =
            Product(
                id = productId,
                name = "Milk",
                brand = "Brand",
                barcode = "4601234567890",
                category = ProductCategory.DAIRY,
                categoryId = "system-dairy",
                quantity = 1.0,
                quantityUnit = QuantityUnit.PIECES,
                remainingAmount = 0.5,
                lowStockThreshold = 0.25,
                expirationDate = LocalDate.parse("2026-05-20"),
                expirationStatus = ExpirationStatus.FRESH,
                householdId = householdId,
                addedByUserId = "user-id",
                createdAt = "2026-05-14T00:00:00Z"
            )

        override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product =
            error("unused")
        override suspend fun updateProduct(
            householdId: String,
            productId: String,
            name: String?,
            category: ProductCategory?,
            categoryId: String?,
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
        ): Product {
            updateRequests += UpdateRequest(
                householdId = householdId,
                productId = productId,
                name = name,
                quantity = quantity,
                remainingAmount = remainingAmount
            )
            return getProduct(householdId, productId).copy(
                name = name ?: "Milk",
                quantity = quantity ?: 1.0,
                remainingAmount = remainingAmount ?: 0.5
            )
        }

        override suspend fun deleteProduct(householdId: String, productId: String) = Unit
        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()
        override suspend fun suggestProductEnrichment(
            householdId: String,
            name: String?,
            brand: String?,
            barcode: String?,
            ingredientsText: String?
        ): ProductEnrichmentSuggestion =
            ProductEnrichmentSuggestion(
                categoryId = "system-dairy",
                category = ProductCategory.DAIRY,
                categoryName = "Молочные",
                confidence = 0.82,
                source = ProductEnrichmentSource.RULE_BASED,
                suggestedName = "Milk normalized",
                suggestedBrand = "Brand",
                suggestedIngredientsText = "Milk",
                calories = 60.0,
                protein = 3.0,
                fat = 2.5,
                carbs = 4.7
            )

        override suspend fun upsertCachedProduct(product: Product) = Unit
        override suspend fun deleteCachedProduct(productId: String) = Unit
    }

    private data class Request(
        val householdId: String,
        val name: String,
        val brand: String?,
        val barcode: String?,
        val category: ProductCategory,
        val categoryId: String?,
        val quantity: Double,
        val quantityUnit: QuantityUnit,
        val expirationDate: LocalDate?,
        val packageAmount: Double?,
        val packageUnit: QuantityUnit?,
        val remainingAmount: Double?,
        val lowStockThreshold: Double?
    )

    private data class UpdateRequest(
        val householdId: String,
        val productId: String,
        val name: String?,
        val quantity: Double?,
        val remainingAmount: Double?
    )

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            ProductCategoryOption.systemDefaults()

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
            ProductCategoryOption(
                id = "custom-id",
                householdId = householdId,
                name = name,
                system = false,
                createdAt = "2026-05-14T00:00:00Z"
            )

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
            error("unused")

        override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
    }
}
