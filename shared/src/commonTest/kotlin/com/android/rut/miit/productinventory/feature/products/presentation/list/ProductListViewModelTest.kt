package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.RealtimeRepository
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

class ProductListViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `applies category and inventory filters to loaded products`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProductRepository(
                products = listOf(
                    product(id = "milk", category = ProductCategory.DAIRY, remainingAmount = 1.0, lowStockThreshold = 2.0),
                    product(id = "juice", category = ProductCategory.BEVERAGES, remainingAmount = 5.0, lowStockThreshold = 2.0)
                )
            )
            val viewModel = viewModel(repository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProductListEvent.OnCategoryFilterChanged(ProductCategory.DAIRY))
            viewModel.onEvent(ProductListEvent.OnInventoryFilterChanged(InventoryFilter.LOW_STOCK))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("milk"), state.visibleProducts.map { it.id })
            assertEquals(ProductCategory.DAIRY, state.filters.category)
            assertEquals(InventoryFilter.LOW_STOCK, state.filters.inventory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `upserts and deletes products from realtime events`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val realtimeRepository = FakeRealtimeRepository()
            val viewModel = viewModel(
                productRepository = FakeProductRepository(products = listOf(product(id = "milk"))),
                realtimeRepository = realtimeRepository
            )

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            realtimeRepository.emit(
                HouseholdRealtimeEvent.ProductCreated(
                    householdId = "household-id",
                    occurredAt = "2026-05-14T00:00:00Z",
                    product = product(id = "bread", category = ProductCategory.CEREALS)
                )
            )
            advanceUntilIdle()
            realtimeRepository.emit(
                HouseholdRealtimeEvent.ProductDeleted(
                    householdId = "household-id",
                    occurredAt = "2026-05-14T00:00:00Z",
                    productId = "milk"
                )
            )
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("bread"), state.products.map { it.id })
            assertEquals(true, state.isRealtimeActive)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `reloads products on resync realtime event`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(products = listOf(product(id = "old")))
            val realtimeRepository = FakeRealtimeRepository()
            val viewModel = viewModel(productRepository, realtimeRepository)

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            productRepository.products = listOf(product(id = "fresh"))
            realtimeRepository.emit(
                HouseholdRealtimeEvent.ResyncRequired(
                    householdId = "household-id",
                    occurredAt = "2026-05-14T00:00:00Z",
                    reason = "missed"
                )
            )
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("fresh"), state.products.map { it.id })
            assertEquals(2, productRepository.getProductsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        productRepository: ProductRepository,
        realtimeRepository: RealtimeRepository
    ): ProductListViewModel =
        ProductListViewModel(
            getProductsUseCase = GetProductsUseCase(productRepository),
            deleteProductUseCase = DeleteProductUseCase(productRepository),
            observeHouseholdEventsUseCase = ObserveHouseholdEventsUseCase(realtimeRepository)
        )

    private class FakeRealtimeRepository : RealtimeRepository {
        private val events = MutableSharedFlow<HouseholdRealtimeEvent>()

        override fun observeHouseholdEvents(householdId: String) = events.asSharedFlow()

        suspend fun emit(event: HouseholdRealtimeEvent) {
            events.emit(event)
        }
    }

    private class FakeProductRepository(
        var products: List<Product>
    ) : ProductRepository {
        var getProductsCalls = 0

        override suspend fun getProducts(householdId: String): List<Product> {
            getProductsCalls += 1
            return products
        }

        override suspend fun deleteProduct(householdId: String, productId: String) {
            products = products.filterNot { it.id == productId }
        }

        override suspend fun getProduct(householdId: String, productId: String): Product = error("unused")
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
        ): Product = error("unused")

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

        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()
    }

    private fun product(
        id: String,
        category: ProductCategory = ProductCategory.DAIRY,
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
