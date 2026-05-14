package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiEvent
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory

sealed class ProductListEvent : UiEvent {
    data class OnCreate(val householdId: String) : ProductListEvent()
    data object OnRetry : ProductListEvent()
    data class OnProductClick(val productId: String) : ProductListEvent()
    data class OnDeleteProduct(val productId: String) : ProductListEvent()
    data class OnCategoryFilterChanged(val category: ProductCategory?) : ProductListEvent()
    data class OnInventoryFilterChanged(val filter: InventoryFilter) : ProductListEvent()
    data object OnAddProductClick : ProductListEvent()
}
