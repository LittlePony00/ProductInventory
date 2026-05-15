package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.RealtimeRepository
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelRealtimeTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun appliesRealtimeCreateUpdateAndDeleteEventsToContentState() = runTest {
        val products = FakeProductRepository(listOf(product("p1", name = "Milk")))
        val realtime = FakeRealtimeRepository()
        val viewModel = ProductListViewModel(
            getProductsUseCase = GetProductsUseCase(products),
            getProductCategoriesUseCase = GetProductCategoriesUseCase(FakeCategoryRepository()),
            deleteProductUseCase = DeleteProductUseCase(products),
            applyRealtimeProductEventUseCase = ApplyRealtimeProductEventUseCase(products),
            observeHouseholdEventsUseCase = ObserveHouseholdEventsUseCase(realtime)
        )

        viewModel.onEvent(ProductListEvent.OnCreate("h1"))
        advanceUntilIdle()

        assertEquals(listOf("Milk"), viewModel.productNames())

        realtime.emit(HouseholdRealtimeEvent.ProductCreated("h1", "now", product("p2", name = "Eggs")))
        advanceUntilIdle()
        assertEquals(listOf("Milk", "Eggs"), viewModel.productNames())
        assertEquals(listOf("Milk", "Eggs"), products.cachedProductNames())

        realtime.emit(HouseholdRealtimeEvent.ProductUpdated("h1", "now", product("p2", name = "Cheese")))
        advanceUntilIdle()
        assertEquals(listOf("Milk", "Cheese"), viewModel.productNames())
        assertEquals(listOf("Milk", "Cheese"), products.cachedProductNames())

        realtime.emit(HouseholdRealtimeEvent.ProductDeleted("h1", "now", "p1"))
        advanceUntilIdle()
        assertEquals(listOf("Cheese"), viewModel.productNames())
        assertEquals(listOf("Cheese"), products.cachedProductNames())
    }

    @Test
    fun resyncEventReloadsProductsFromRepository() = runTest {
        val products = FakeProductRepository(listOf(product("p1", name = "Milk")))
        val realtime = FakeRealtimeRepository()
        val viewModel = ProductListViewModel(
            getProductsUseCase = GetProductsUseCase(products),
            getProductCategoriesUseCase = GetProductCategoriesUseCase(FakeCategoryRepository()),
            deleteProductUseCase = DeleteProductUseCase(products),
            applyRealtimeProductEventUseCase = ApplyRealtimeProductEventUseCase(products),
            observeHouseholdEventsUseCase = ObserveHouseholdEventsUseCase(realtime)
        )

        viewModel.onEvent(ProductListEvent.OnCreate("h1"))
        advanceUntilIdle()
        products.products = listOf(product("p3", name = "Bread"))

        realtime.emit(HouseholdRealtimeEvent.ResyncRequired("h1", "now"))
        advanceUntilIdle()

        assertEquals(listOf("Bread"), viewModel.productNames())
    }

    private fun ProductListViewModel.productNames(): List<String> {
        val state = viewState.value as ProductListState.Content
        return state.products.map { it.name }
    }

    private fun product(id: String, name: String) = Product(
        id = id,
        name = name,
        category = ProductCategory.OTHER,
        quantity = 1.0,
        quantityUnit = QuantityUnit.PIECES,
        expirationDate = LocalDate.parse("2026-05-20"),
        expirationStatus = ExpirationStatus.FRESH,
        householdId = "h1",
        addedByUserId = "u1",
        createdAt = "2026-05-14T00:00:00Z"
    )
}

private class FakeRealtimeRepository : RealtimeRepository {
    private val events = MutableSharedFlow<HouseholdRealtimeEvent>()

    override fun observeHouseholdEvents(householdId: String): Flow<HouseholdRealtimeEvent> = events

    suspend fun emit(event: HouseholdRealtimeEvent) {
        events.emit(event)
    }
}

private class FakeProductRepository(
    var products: List<Product>
) : ProductRepository {
    private var cachedProducts = products

    override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> =
        categoryId?.let { id -> products.filter { it.categoryId == id } } ?: products

    override suspend fun deleteProduct(householdId: String, productId: String) {
        products = products.filterNot { it.id == productId }
    }

    override suspend fun getProduct(householdId: String, productId: String): Product =
        error("Unused")

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
    ): Product = error("Unused")

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
    ): Product = error("Unused")

    override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> =
        emptyList()

    override suspend fun suggestProductEnrichment(
        householdId: String,
        name: String?,
        brand: String?,
        barcode: String?,
        ingredientsText: String?
    ): com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion =
        error("Unused")

    override suspend fun upsertCachedProduct(product: Product) {
        cachedProducts = cachedProducts.filterNot { it.id == product.id } + product
    }

    override suspend fun deleteCachedProduct(productId: String) {
        cachedProducts = cachedProducts.filterNot { it.id == productId }
    }

    fun cachedProductNames(): List<String> = cachedProducts.map { it.name }
}

private class FakeCategoryRepository : CategoryRepository {
    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
        ProductCategoryOption.systemDefaults()

    override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
        error("Unused")

    override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
        error("Unused")

    override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
}
