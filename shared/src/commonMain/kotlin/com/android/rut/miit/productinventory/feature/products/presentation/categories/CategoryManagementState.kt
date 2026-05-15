package com.android.rut.miit.productinventory.feature.products.presentation.categories

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

data class CategoryManagementState(
    val categories: List<ProductCategoryOption> = emptyList(),
    val editableNames: Map<String, String> = emptyMap(),
    val newCategoryName: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
) : UiState
