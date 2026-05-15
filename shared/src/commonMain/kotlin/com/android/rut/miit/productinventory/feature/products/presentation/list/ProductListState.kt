package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

sealed class ProductListState : UiState {
    data object Loading : ProductListState()
    data class Content(
        val products: List<Product>,
        val categories: List<ProductCategoryOption>,
        val visibleProducts: List<Product> = products,
        val filters: ProductListFilters = ProductListFilters(),
        val isRealtimeActive: Boolean = false
    ) : ProductListState()
    data class Error(val message: String?) : ProductListState()
}
