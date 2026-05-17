package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminder
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

sealed class ProductListState : UiState {
    data object Loading : ProductListState()
    data class Content(
        val products: List<Product>,
        val categories: List<ProductCategoryOption>,
        val visibleProducts: List<Product> = products,
        val filters: ProductListFilters = ProductListFilters(),
        val isRealtimeActive: Boolean = false,
        val localReminders: List<ProductLocalReminder> = emptyList(),
        val notificationSettings: NotificationSettings = NotificationSettings()
    ) : ProductListState()
    data class Error(val message: String?) : ProductListState()
}
