package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.Product

sealed class ProductListState : UiState {
    data object Loading : ProductListState()
    data class Content(
        val products: List<Product>,
        val visibleProducts: List<Product> = products,
        val filters: ProductListFilters = ProductListFilters(),
        val isRealtimeActive: Boolean = false
    ) : ProductListState()
    data class Error(val message: String?) : ProductListState()
}
