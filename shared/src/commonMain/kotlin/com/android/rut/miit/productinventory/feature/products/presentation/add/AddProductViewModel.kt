package com.android.rut.miit.productinventory.feature.products.presentation.add

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class AddProductViewModel(
    private val addProductUseCase: AddProductUseCase
) : SharedViewModel<AddProductState, AddProductEvent, AddProductAction>(
    initialState = AddProductState()
) {

    var householdId: String = ""

    override suspend fun handleEvent(event: AddProductEvent) {
        when (event) {
            is AddProductEvent.OnScannedDraftApplied ->
                applyScannedDraft(event)
            is AddProductEvent.OnNameChanged ->
                updateState { copy(name = event.name) }
            is AddProductEvent.OnBrandChanged ->
                updateState { copy(brand = event.brand) }
            is AddProductEvent.OnBarcodeChanged ->
                updateState { copy(barcode = event.barcode) }
            is AddProductEvent.OnCategoryChanged ->
                updateState { copy(category = event.category) }
            is AddProductEvent.OnQuantityChanged ->
                updateState { copy(quantity = event.quantity) }
            is AddProductEvent.OnQuantityUnitChanged ->
                updateState { copy(quantityUnit = event.unit) }
            is AddProductEvent.OnPackageAmountChanged ->
                updateState { copy(packageAmount = event.amount) }
            is AddProductEvent.OnPackageUnitChanged ->
                updateState { copy(packageUnit = event.unit) }
            is AddProductEvent.OnIngredientsChanged ->
                updateState { copy(ingredientsText = event.ingredients) }
            is AddProductEvent.OnCaloriesChanged ->
                updateState { copy(calories = event.calories) }
            is AddProductEvent.OnProteinChanged ->
                updateState { copy(protein = event.protein) }
            is AddProductEvent.OnFatChanged ->
                updateState { copy(fat = event.fat) }
            is AddProductEvent.OnCarbsChanged ->
                updateState { copy(carbs = event.carbs) }
            is AddProductEvent.OnExpirationDateChanged ->
                updateState { copy(expirationDate = event.date) }
            is AddProductEvent.OnSaveClick -> save()
            is AddProductEvent.OnBackClick ->
                sendAction(AddProductAction.NavigateBack)
        }
    }

    private fun applyScannedDraft(event: AddProductEvent.OnScannedDraftApplied) {
        updateState {
            copy(
                barcode = event.barcode,
                name = event.name ?: name,
                brand = event.brand ?: brand,
                category = event.category ?: category,
                packageAmount = event.packageAmount ?: packageAmount,
                packageUnit = event.packageUnit ?: packageUnit,
                ingredientsText = event.ingredientsText ?: ingredientsText,
                calories = event.calories ?: calories,
                protein = event.protein ?: protein,
                fat = event.fat ?: fat,
                carbs = event.carbs ?: carbs
            )
        }
    }

    private fun save() {
        val state = currentState
        if (state.name.isBlank()) {
            sendAction(AddProductAction.ShowError("Введите название продукта"))
            return
        }
        val qty = state.quantity.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            sendAction(AddProductAction.ShowError("Введите корректное количество"))
            return
        }

        val expDate = state.expirationDate.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        val packageAmount = state.packageAmount.toDoubleOrNull()
        val calories = state.calories.toDoubleOrNull()
        val protein = state.protein.toDoubleOrNull()
        val fat = state.fat.toDoubleOrNull()
        val carbs = state.carbs.toDoubleOrNull()

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                addProductUseCase(
                    householdId = householdId,
                    name = state.name,
                    category = state.category,
                    quantity = qty,
                    quantityUnit = state.quantityUnit,
                    expirationDate = expDate,
                    brand = state.brand.takeIf { it.isNotBlank() },
                    barcode = state.barcode.takeIf { it.isNotBlank() },
                    packageAmount = packageAmount,
                    packageUnit = state.packageUnit.takeIf { packageAmount != null },
                    ingredientsText = state.ingredientsText.takeIf { it.isNotBlank() },
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs
                )
            }.onSuccess {
                updateState { copy(isLoading = false) }
                sendAction(AddProductAction.ProductAdded)
            }.onFailure { error ->
                updateState { copy(isLoading = false, error = error.message) }
                sendAction(AddProductAction.ShowError(error.message ?: "Ошибка"))
            }
        }
    }
}
