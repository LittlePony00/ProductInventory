package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit

data class AddProductState(
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val category: ProductCategory = ProductCategory.OTHER,
    val categoryId: String? = null,
    val categories: List<ProductCategoryOption> = ProductCategoryOption.systemDefaults(),
    val newCategoryName: String = "",
    val isCreatingCategory: Boolean = false,
    val isSuggestingProduct: Boolean = false,
    val suggestionMessage: String? = null,
    val quantity: String = "",
    val quantityUnit: QuantityUnit = QuantityUnit.PIECES,
    val remainingAmount: String = "",
    val lowStockThreshold: String = "",
    val expirationDate: String = "",
    val packageAmount: String = "",
    val packageUnit: QuantityUnit = QuantityUnit.PIECES,
    val ingredientsText: String = "",
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val isImageRemoved: Boolean = false,
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val isBarcodePrefilled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) : UiState
