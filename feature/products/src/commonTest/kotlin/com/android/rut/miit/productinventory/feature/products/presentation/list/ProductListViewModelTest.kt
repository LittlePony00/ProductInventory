package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.ConsumeProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
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
                    product(
                        id = "milk",
                        category = ProductCategory.DAIRY,
                        categoryId = "dairy",
                        remainingAmount = 1.0,
                        lowStockThreshold = 2.0
                    ),
                    product(
                        id = "juice",
                        category = ProductCategory.BEVERAGES,
                        categoryId = "beverages",
                        remainingAmount = 5.0,
                        lowStockThreshold = 2.0
                    )
                )
            )
            val viewModel = viewModel(repository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProductListEvent.OnCategoryFilterChanged("dairy"))
            viewModel.onEvent(ProductListEvent.OnInventoryFilterChanged(InventoryFilter.LOW_STOCK))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("milk"), state.visibleProducts.map { it.id })
            assertEquals("dairy", state.filters.categoryId)
            assertEquals(InventoryFilter.LOW_STOCK, state.filters.inventory)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `system category filter matches products without category id`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProductRepository(
                products = listOf(
                    product(
                        id = "milk",
                        category = ProductCategory.DAIRY,
                        categoryId = null
                    )
                )
            )
            val viewModel = viewModel(repository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProductListEvent.OnCategoryFilterChanged(ProductCategoryOption.DAIRY_SYSTEM_ID))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("milk"), state.visibleProducts.map { it.id })
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
            productRepository.products = listOf(product(id = "fresh", imageUrl = "https://cdn.example.test/fresh.jpg"))
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
            assertEquals("https://cdn.example.test/fresh.jpg", state.products.single().imageUrl)
            assertEquals(1, productRepository.getProductsCalls)
            assertEquals(2, productRepository.refreshProductsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `consume product updates content without reloading full list`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(
                products = listOf(product(id = "milk", remainingAmount = 2.0))
            )
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProductListEvent.OnConsumeProduct(productId = "milk", amount = 0.75))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(1.25, state.products.single().remainingAmount)
            assertEquals(1, productRepository.getProductsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `delete product removes item from content without reloading full list`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(
                products = listOf(product(id = "milk"), product(id = "bread", category = ProductCategory.CEREALS))
            )
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProductListEvent.OnDeleteProduct(productId = "milk"))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("bread"), state.products.map { it.id })
            assertEquals(1, productRepository.getProductsCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `resume refresh is silent and does not show sync progress or error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(products = listOf(product(id = "milk")))
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            productRepository.refreshProductsError = IllegalStateException("offline")
            viewModel.onEvent(ProductListEvent.OnResume)
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("milk"), state.products.map { it.id })
            assertEquals(false, state.isRefreshing)
            assertEquals(null, state.syncErrorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `resume refreshes content with latest remote image url`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(products = listOf(product(id = "milk")))
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            productRepository.products = listOf(
                product(id = "milk", imageUrl = "http://10.8.0.2:9000/product-images/products/milk.jpg")
            )
            viewModel.onEvent(ProductListEvent.OnResume)
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(
                "http://10.8.0.2:9000/product-images/products/milk.jpg",
                state.products.single().imageUrl
            )
            assertEquals(false, state.isRefreshing)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `resume retries product load after transient error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(products = listOf(product(id = "milk"))).apply {
                getProductsError = IllegalStateException("offline")
            }
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()
            assertIs<ProductListState.Error>(viewModel.viewState.value)

            productRepository.getProductsError = null
            viewModel.onEvent(ProductListEvent.OnResume)
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("milk"), state.products.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `plans local Russian reminders from loaded products and notification settings`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(
                products = listOf(
                    product(
                        id = "milk",
                        remainingAmount = 1.0,
                        lowStockThreshold = 2.0,
                        expirationDate = LocalDate.parse("2026-05-20")
                    )
                )
            )
            val viewModel = viewModel(
                productRepository = productRepository,
                realtimeRepository = FakeRealtimeRepository(),
                notificationRepository = FakeNotificationRepository(
                    settings = NotificationSettings(expirationReminderDays = 2)
                )
            )

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(
                listOf("Скоро истекает срок годности", "Продукт заканчивается"),
                state.localReminders.map { it.title }
            )
            assertEquals(
                "Срок годности продукта «milk» истекает 2026-05-20",
                state.localReminders.first().message
            )
            assertEquals("2026-05-18", state.localReminders.first().triggerDateIso)
            assertEquals("Осталось: «milk» — 1.0 шт.", state.localReminders.last().message)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `plans local reminders with default settings when notification settings are unavailable offline`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(
                products = listOf(
                    product(
                        id = "offline-milk",
                        expirationDate = LocalDate.parse("2026-05-20")
                    )
                )
            )
            val viewModel = viewModel(
                productRepository = productRepository,
                realtimeRepository = FakeRealtimeRepository(),
                notificationRepository = FakeNotificationRepository(throwOnGetSettings = true)
            )

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Скоро истекает срок годности"), state.localReminders.map { it.title })
            assertEquals("2026-05-17", state.localReminders.single().triggerDateIso)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `cold offline start with empty product cache shows empty content`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val productRepository = FakeProductRepository(products = emptyList()).apply {
                refreshProductsError = IllegalStateException("offline")
            }
            val viewModel = viewModel(productRepository, FakeRealtimeRepository())

            viewModel.onEvent(ProductListEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<ProductListState.Content>(viewModel.viewState.value)
            assertEquals(emptyList(), state.products)
            assertEquals(ProductCategoryOption.systemDefaults().map { it.id }, state.categories.map { it.id })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        productRepository: ProductRepository,
        realtimeRepository: RealtimeRepository,
        notificationRepository: NotificationRepository = FakeNotificationRepository()
    ): ProductListViewModel =
        ProductListViewModel(
            getProductsUseCase = GetProductsUseCase(productRepository),
            refreshProductsUseCase = RefreshProductsUseCase(productRepository),
            getProductCategoriesUseCase = GetProductCategoriesUseCase(FakeCategoryRepository()),
            refreshProductCategoriesUseCase = RefreshProductCategoriesUseCase(FakeCategoryRepository()),
            deleteProductUseCase = DeleteProductUseCase(productRepository),
            consumeProductUseCase = ConsumeProductUseCase(productRepository),
            applyRealtimeProductEventUseCase = ApplyRealtimeProductEventUseCase(productRepository),
            observeHouseholdEventsUseCase = ObserveHouseholdEventsUseCase(realtimeRepository),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(notificationRepository)
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
        var refreshProductsCalls = 0
        var getProductsError: Throwable? = null
        var refreshProductsError: Throwable? = null

        override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> {
            getProductsCalls += 1
            getProductsError?.let { throw it }
            return categoryId?.let { id -> products.filter { it.categoryId == id } } ?: products
        }

        override suspend fun refreshProducts(householdId: String, categoryId: String?): List<Product> {
            refreshProductsCalls += 1
            refreshProductsError?.let { throw it }
            return categoryId?.let { id -> products.filter { it.categoryId == id } } ?: products
        }

        override suspend fun deleteProduct(householdId: String, productId: String) {
            products = products.filterNot { it.id == productId }
        }

        override suspend fun getProduct(householdId: String, productId: String): Product = error("unused")
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
        ): Product = error("unused")

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
        ): Product = error("unused")

        override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product {
            val existing = products.first { it.id == productId }
            val updated = existing.copy(remainingAmount = existing.remainingAmount - amount)
            products = products.map { if (it.id == productId) updated else it }
            return updated
        }

        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()
        override suspend fun suggestProductEnrichment(
            householdId: String,
            name: String?,
            brand: String?,
            barcode: String?,
            ingredientsText: String?
        ): com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion =
            error("unused")

        override suspend fun upsertCachedProduct(product: Product) {
            products = products.filterNot { it.id == product.id } + product
        }

        override suspend fun deleteCachedProduct(productId: String) {
            products = products.filterNot { it.id == productId }
        }
    }

    private fun product(
        id: String,
        category: ProductCategory = ProductCategory.DAIRY,
        categoryId: String? = category.name.lowercase(),
        remainingAmount: Double = 1.0,
        lowStockThreshold: Double? = null,
        expirationStatus: ExpirationStatus = ExpirationStatus.FRESH,
        expirationDate: LocalDate? = null,
        imageUrl: String? = null,
        localImagePath: String? = null
    ): Product =
        Product(
            id = id,
            name = id,
            category = category,
            categoryId = categoryId,
            quantity = 10.0,
            quantityUnit = QuantityUnit.PIECES,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            expirationDate = expirationDate,
            expirationStatus = expirationStatus,
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        )

    private class FakeNotificationRepository(
        private val settings: NotificationSettings = NotificationSettings(),
        private val throwOnGetSettings: Boolean = false
    ) : NotificationRepository {
        override suspend fun getNotifications(): List<Notification> = emptyList()
        override suspend fun getUnreadNotifications(): List<Notification> = emptyList()
        override suspend fun markAsRead(notificationId: String) = Unit
        override suspend fun markAllAsRead() = Unit
        override suspend fun getSettings(): NotificationSettings {
            if (throwOnGetSettings) error("offline")
            return settings
        }
        override suspend fun updateSettings(
            expirationRemindersEnabled: Boolean?,
            lowStockRemindersEnabled: Boolean?,
            pushEnabled: Boolean?,
            expirationReminderDays: Int?
        ): NotificationSettings = settings
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            ProductCategoryOption.systemDefaults()

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
            error("unused")

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
            error("unused")

        override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
    }
}
