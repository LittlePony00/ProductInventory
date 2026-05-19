package com.android.rut.miit.productinventory.feature.products.presentation.categories

import com.android.rut.miit.productinventory.common.UiAction

sealed class CategoryManagementAction : UiAction {
    data class ShowError(val message: String) : CategoryManagementAction()
}
