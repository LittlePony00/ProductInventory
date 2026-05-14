package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.common.UiEvent
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit

sealed class AddProductEvent : UiEvent {
    data class OnPrefill(
        val barcode: String? = null,
        val name: String? = null,
        val brand: String? = null,
        val category: ProductCategory? = null,
        val quantity: String? = null,
        val quantityUnit: QuantityUnit? = null,
        val packageAmount: String? = null,
        val packageUnit: QuantityUnit? = null,
        val ingredientsText: String? = null,
        val calories: String? = null,
        val protein: String? = null,
        val fat: String? = null,
        val carbs: String? = null
    ) : AddProductEvent()

    data class OnScannedDraftApplied(
        val barcode: String,
        val name: String?,
        val brand: String?,
        val category: ProductCategory?,
        val packageAmount: String?,
        val packageUnit: QuantityUnit?,
        val ingredientsText: String?,
        val calories: String?,
        val protein: String?,
        val fat: String?,
        val carbs: String?
    ) : AddProductEvent()

    data class OnNameChanged(val name: String) : AddProductEvent()
    data class OnBrandChanged(val brand: String) : AddProductEvent()
    data class OnBarcodeChanged(val barcode: String) : AddProductEvent()
    data class OnCategoryChanged(val category: ProductCategory) : AddProductEvent()
    data class OnQuantityChanged(val quantity: String) : AddProductEvent()
    data class OnQuantityUnitChanged(val unit: QuantityUnit) : AddProductEvent()
    data class OnRemainingAmountChanged(val amount: String) : AddProductEvent()
    data class OnLowStockThresholdChanged(val threshold: String) : AddProductEvent()
    data class OnExpirationDateChanged(val date: String) : AddProductEvent()
    data class OnPackageAmountChanged(val amount: String) : AddProductEvent()
    data class OnPackageUnitChanged(val unit: QuantityUnit) : AddProductEvent()
    data class OnIngredientsChanged(val ingredients: String) : AddProductEvent()
    data class OnCaloriesChanged(val calories: String) : AddProductEvent()
    data class OnProteinChanged(val protein: String) : AddProductEvent()
    data class OnFatChanged(val fat: String) : AddProductEvent()
    data class OnCarbsChanged(val carbs: String) : AddProductEvent()
    data object OnSaveClick : AddProductEvent()
    data object OnBackClick : AddProductEvent()
}
