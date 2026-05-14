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

    override suspend fun handleEvent(event: ProductListEvent) {
        when (event) {
            is ProductListEvent.OnCreate -> onCreate(event.householdId)
            is ProductListEvent.OnRetry -> loadProducts()
            is ProductListEvent.OnProductClick -> onProductClick(event.productId)
            is ProductListEvent.OnDeleteProduct -> onDeleteProduct(event.productId)
            is ProductListEvent.OnAddProductClick -> sendAction(ProductListAction.OpenAddProduct)
        }
    }

    private fun onCreate(householdId: String) {
        this.householdId = householdId
        loadProducts()
        observeRealtimeUpdates(householdId)
    }

    private fun loadProducts() {
        viewModelScope.launch {
            updateState { ProductListState.Loading }
            runCatching { getProductsUseCase(householdId) }
                .onSuccess { products -> updateState { ProductListState.Content(products) } }
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

    private fun observeRealtimeUpdates(householdId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            observeHouseholdEventsUseCase(householdId).collect { event ->
                if (event.householdId == this@ProductListViewModel.householdId) {
                    applyRealtimeProductEventUseCase(event)
                    applyRealtimeEvent(event)
                }
            }
        }
    }

    private fun applyRealtimeEvent(event: HouseholdRealtimeEvent) {
        when (event) {
            is HouseholdRealtimeEvent.ProductCreated -> upsertProduct(event.product)
            is HouseholdRealtimeEvent.ProductUpdated -> upsertProduct(event.product)
            is HouseholdRealtimeEvent.ProductDeleted -> updateContentProducts { products ->
                products.filterNot { it.id == event.productId }
            }
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

    private fun updateContentProducts(transform: (List<Product>) -> List<Product>) {
        val state = currentState
        if (state is ProductListState.Content) {
            updateState { ProductListState.Content(transform(state.products)) }
        }
    }
}
