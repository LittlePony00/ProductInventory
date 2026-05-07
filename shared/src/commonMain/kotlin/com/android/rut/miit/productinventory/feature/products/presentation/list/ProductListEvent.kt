package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiEvent

sealed class ProductListEvent : UiEvent {
    data class OnCreate(val householdId: String) : ProductListEvent()
    data object OnRetry : ProductListEvent()
    data class OnProductClick(val productId: String) : ProductListEvent()
    data class OnDeleteProduct(val productId: String) : ProductListEvent()
    data object OnAddProductClick : ProductListEvent()
}
