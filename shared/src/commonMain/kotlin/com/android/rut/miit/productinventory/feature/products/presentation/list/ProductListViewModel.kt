package com.android.rut.miit.productinventory.feature.products.presentation.list

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val applyRealtimeProductEventUseCase: ApplyRealtimeProductEventUseCase,
    private val observeHouseholdEventsUseCase: ObserveHouseholdEventsUseCase
) : SharedViewModel<ProductListState, ProductListEvent, ProductListAction>(
    initialState = ProductListState.Loading
) {

    private var householdId: String = ""
    private var realtimeJob: Job? = null
    private var filters = ProductListFilters()

    override suspend fun handleEvent(event: ProductListEvent) {
        when (event) {
            is ProductListEvent.OnCreate -> onCreate(event.householdId)
            is ProductListEvent.OnRetry -> loadProducts()
            is ProductListEvent.OnProductClick -> onProductClick(event.productId)
            is ProductListEvent.OnDeleteProduct -> onDeleteProduct(event.productId)
            is ProductListEvent.OnCategoryFilterChanged -> updateFilters(filters.copy(category = event.category))
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

    private fun loadProducts() {
        viewModelScope.launch {
            updateState { ProductListState.Loading }
            runCatching { getProductsUseCase(householdId) }
                .onSuccess { products -> showContent(products) }
                .onFailure { error -> updateState { ProductListState.Error(error.message) } }
        }
    }

    private fun onProductClick(productId: String) {
        sendAction(ProductListAction.OpenProductDetail(productId))
    }

    private fun onDeleteProduct(productId: String) {
        viewModelScope.launch {
            runCatching { deleteProductUseCase(householdId, productId) }
                .onSuccess { loadProducts() }
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
                    runCatching { applyRealtimeProductEventUseCase(event) }
                    handleRealtimeEvent(event)
                }
        }
    }

    private fun handleRealtimeEvent(event: HouseholdRealtimeEvent) {
        when (event) {
            is HouseholdRealtimeEvent.ProductCreated -> upsertProduct(event.product)
            is HouseholdRealtimeEvent.ProductUpdated -> upsertProduct(event.product)
            is HouseholdRealtimeEvent.ProductDeleted -> removeProduct(event.productId)
            is HouseholdRealtimeEvent.ResyncRequired -> loadProducts()
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
                        visibleProducts = nextProducts.applyFilters(filters),
                        filters = filters
                    )
                }
                else -> this
            }
        }
    }

    private fun showContent(products: List<Product>) {
        val isRealtimeActive = currentState is ProductListState.Content &&
            (currentState as ProductListState.Content).isRealtimeActive
        updateState {
            ProductListState.Content(
                products = products,
                visibleProducts = products.applyFilters(filters),
                filters = filters,
                isRealtimeActive = isRealtimeActive
            )
        }
    }
}
