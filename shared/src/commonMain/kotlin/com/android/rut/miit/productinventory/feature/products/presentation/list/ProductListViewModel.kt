package com.android.rut.miit.productinventory.feature.products.presentation.list

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : SharedViewModel<ProductListState, ProductListEvent, ProductListAction>(
    initialState = ProductListState.Loading
) {

    private var householdId: String = ""

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
}
