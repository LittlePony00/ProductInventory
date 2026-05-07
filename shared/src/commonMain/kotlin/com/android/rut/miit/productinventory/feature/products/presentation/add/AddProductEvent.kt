package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.common.UiEvent
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit

sealed class AddProductEvent : UiEvent {
    data class OnNameChanged(val name: String) : AddProductEvent()
    data class OnCategoryChanged(val category: ProductCategory) : AddProductEvent()
    data class OnQuantityChanged(val quantity: String) : AddProductEvent()
    data class OnQuantityUnitChanged(val unit: QuantityUnit) : AddProductEvent()
    data class OnExpirationDateChanged(val date: String) : AddProductEvent()
    data object OnSaveClick : AddProductEvent()
    data object OnBackClick : AddProductEvent()
}
