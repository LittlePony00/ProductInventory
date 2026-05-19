package com.android.rut.miit.productinventory.feature.products.presentation.list

import com.android.rut.miit.productinventory.common.UiAction

sealed class ProductListAction : UiAction {
    data class OpenProductDetail(val productId: String) : ProductListAction()
    data object OpenAddProduct : ProductListAction()
}
