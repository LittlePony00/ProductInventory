package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.common.UiAction

sealed class AddProductAction : UiAction {
    data object ProductAdded : AddProductAction()
    data object NavigateBack : AddProductAction()
    data class ShowError(val message: String) : AddProductAction()
}
