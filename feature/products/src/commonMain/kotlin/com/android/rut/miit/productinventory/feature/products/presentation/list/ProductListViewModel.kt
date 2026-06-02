package com.android.rut.miit.productinventory.feature.products.presentation.list

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.ConsumeProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminderPlanner
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val refreshProductsUseCase: RefreshProductsUseCase,
    private val getProductCategoriesUseCase: GetProductCategoriesUseCase,
    private val refreshProductCategoriesUseCase: RefreshProductCategoriesUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val consumeProductUseCase: ConsumeProductUseCase,
    private val applyRealtimeProductEventUseCase: ApplyRealtimeProductEventUseCase,
    private val observeHouseholdEventsUseCase: ObserveHouseholdEventsUseCase,
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase
) : SharedViewModel<ProductListState, ProductListEvent, ProductListAction>(
    initialState = ProductListState.Loading
) {

    private var householdId: String = ""
    private var realtimeJob: Job? = null
    private var filters = ProductListFilters()
    private val localReminderPlanner = ProductLocalReminderPlanner()

    override suspend fun handleEvent(event: ProductListEvent) {
        when (event) {
            is ProductListEvent.OnCreate -> onCreate(event.householdId)
            is ProductListEvent.OnResume -> onResume()
            is ProductListEvent.OnRetry -> refreshProducts(showRefreshing = true, showError = true)
            is ProductListEvent.OnProductClick -> onProductClick(event.productId)
            is ProductListEvent.OnDeleteProduct -> onDeleteProduct(event.productId)
            is ProductListEvent.OnConsumeProduct -> onConsumeProduct(event.productId, event.amount)
            is ProductListEvent.OnCategoryFilterChanged -> updateFilters(filters.copy(categoryId = event.categoryId))
            is ProductListEvent.OnInventoryFilterChanged -> updateFilters(filters.copy(inventory = event.filter))
            is ProductListEvent.OnAddProductClick -> sendAction(ProductListAction.OpenAddProduct)
        }
    }

    private fun onCreate(householdId: String) {
        if (this.householdId == householdId && currentState is ProductListState.Content) return
        this.householdId = householdId
        filters = ProductListFilters()
        loadProducts()
        observeRealtimeEvents()
    }

    private fun onResume() {
        if (householdId.isNotBlank() && currentState is ProductListState.Content) {
            refreshProducts(showRefreshing = false, showError = false)
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val hadContent = currentState is ProductListState.Content
            updateState {
                when (this) {
                    is ProductListState.Content -> copy(isRefreshing = true, syncErrorMessage = null)
                    else -> ProductListState.Loading
                }
            }
            runCatching {
                coroutineScope {
                    val categories = async { getProductCategoriesUseCase(householdId) }
                    val products = async { getProductsUseCase(householdId) }
                    val settings = async {
                        runCatching { getNotificationSettingsUseCase() }
                            .getOrDefault(NotificationSettings())
                    }
                    ProductsContent(
                        products = products.await(),
                        categories = categories.await(),
                        notificationSettings = settings.await()
                    )
                }
            }
                .onSuccess { content ->
                    showContent(content.products, content.categories, content.notificationSettings)
                    refreshProducts(showRefreshing = false, showError = false)
                }
                .onFailure { error ->
                    updateState {
                        when {
                            hadContent && this is ProductListState.Content -> copy(
                                isRefreshing = false,
                                syncErrorMessage = error.message
                            )
                            else -> ProductListState.Error(error.message)
                        }
                    }
                }
        }
    }

    private fun onProductClick(productId: String) {
        sendAction(ProductListAction.OpenProductDetail(productId))
    }

    private fun onDeleteProduct(productId: String) {
        viewModelScope.launch {
            val previousState = currentState as? ProductListState.Content
            removeProduct(productId)
            runCatching { deleteProductUseCase(householdId, productId) }
                .onFailure { error ->
                    updateState { previousState ?: ProductListState.Error(error.message) }
                }
        }
    }

    private fun onConsumeProduct(productId: String, amount: Double) {
        viewModelScope.launch {
            runCatching { consumeProductUseCase(householdId, productId, amount) }
                .onSuccess { product -> upsertProduct(product) }
                .onFailure { /* silently fail, could show snackbar */ }
        }
    }

    private fun observeRealtimeEvents() {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            observeHouseholdEventsUseCase(householdId)
                .catch {
                    updateState {
                        when (this) {
                            is ProductListState.Content -> copy(isRealtimeActive = false)
                            else -> this
                        }
                    }
                }
                .collect { event ->
                    if (event.householdId != householdId) return@collect
                    updateState {
                        when (this) {
                            is ProductListState.Content -> copy(isRealtimeActive = true)
                            else -> this
                        }
                    }
                    val cachedProduct = runCatching { applyRealtimeProductEventUseCase(event) }.getOrNull()
                    handleRealtimeEvent(event, cachedProduct)
                }
        }
    }

    private fun handleRealtimeEvent(event: HouseholdRealtimeEvent, cachedProduct: Product?) {
        when (event) {
            is HouseholdRealtimeEvent.ProductCreated -> upsertProduct(cachedProduct ?: event.product)
            is HouseholdRealtimeEvent.ProductUpdated -> upsertProduct(cachedProduct ?: event.product)
            is HouseholdRealtimeEvent.ProductDeleted -> removeProduct(event.productId)
            is HouseholdRealtimeEvent.ResyncRequired -> refreshProducts(showRefreshing = false, showError = false)
        }
    }

    private fun refreshProducts(showRefreshing: Boolean, showError: Boolean) {
        viewModelScope.launch {
            val hadContent = currentState is ProductListState.Content
            if (showRefreshing) {
                updateState {
                    when (this) {
                        is ProductListState.Content -> copy(isRefreshing = true, syncErrorMessage = null)
                        else -> ProductListState.Loading
                    }
                }
            }
            runCatching {
                coroutineScope {
                    val categories = async { refreshProductCategoriesUseCase(householdId) }
                    val products = async { refreshProductsUseCase(householdId) }
                    val settings = async {
                        runCatching { getNotificationSettingsUseCase() }
                            .getOrDefault(NotificationSettings())
                    }
                    ProductsContent(
                        products = products.await(),
                        categories = categories.await(),
                        notificationSettings = settings.await()
                    )
                }
            }
                .onSuccess { content ->
                    showContent(content.products, content.categories, content.notificationSettings)
                }
                .onFailure { error ->
                    if (!showError) return@onFailure
                    updateState {
                        when {
                            hadContent && this is ProductListState.Content -> copy(
                                isRefreshing = false,
                                syncErrorMessage = error.message
                            )
                            else -> ProductListState.Error(error.message)
                        }
                    }
                }
        }
    }

    private fun upsertProduct(product: Product) {
        updateContentProducts { products ->
            if (products.any { it.id == product.id }) {
                products.map { if (it.id == product.id) product else it }
            } else {
                products + product
            }
        }
    }

    private fun removeProduct(productId: String) {
        updateContentProducts { products -> products.filterNot { it.id == productId } }
    }

    private fun updateFilters(next: ProductListFilters) {
        filters = next
        updateState {
            when (this) {
                is ProductListState.Content -> copy(
                    visibleProducts = products.applyFilters(next),
                    filters = next
                )
                else -> this
            }
        }
    }

    private fun updateContentProducts(transform: (List<Product>) -> List<Product>) {
        updateState {
            when (this) {
                is ProductListState.Content -> {
                    val nextProducts = transform(products)
                    copy(
                        products = nextProducts,
                        categories = categories,
                        visibleProducts = nextProducts.applyFilters(filters),
                        filters = filters,
                        isRefreshing = false,
                        syncErrorMessage = null,
                        localReminders = localReminderPlanner.plan(nextProducts, notificationSettings)
                    )
                }
                else -> this
            }
        }
    }

    private fun showContent(
        products: List<Product>,
        categories: List<ProductCategoryOption>,
        notificationSettings: NotificationSettings
    ) {
        val isRealtimeActive = currentState is ProductListState.Content &&
            (currentState as ProductListState.Content).isRealtimeActive
        updateState {
            ProductListState.Content(
                products = products,
                categories = categories,
                visibleProducts = products.applyFilters(filters),
                filters = filters,
                isRealtimeActive = isRealtimeActive,
                isRefreshing = false,
                syncErrorMessage = null,
                localReminders = localReminderPlanner.plan(products, notificationSettings),
                notificationSettings = notificationSettings
            )
        }
    }

    private data class ProductsContent(
        val products: List<Product>,
        val categories: List<ProductCategoryOption>,
        val notificationSettings: NotificationSettings
    )
}
