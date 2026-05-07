package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit

data class AddProductState(
    val name: String = "",
    val category: ProductCategory = ProductCategory.OTHER,
    val quantity: String = "",
    val quantityUnit: QuantityUnit = QuantityUnit.PIECES,
    val expirationDate: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState
