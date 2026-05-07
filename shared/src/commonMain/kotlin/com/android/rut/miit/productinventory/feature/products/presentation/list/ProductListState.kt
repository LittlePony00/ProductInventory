package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.Product

sealed class ProductListState : UiState {
    data object Loading : ProductListState()
    data class Content(val products: List<Product>) : ProductListState()
    data class Error(val message: String?) : ProductListState()
}
