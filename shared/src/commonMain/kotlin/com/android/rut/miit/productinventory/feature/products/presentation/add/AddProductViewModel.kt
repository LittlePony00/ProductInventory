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
            is AddProductEvent.OnPrefill -> prefill(event)
            is AddProductEvent.OnScannedDraftApplied -> applyScannedDraft(event)
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
            is AddProductEvent.OnRemainingAmountChanged ->
                updateState { copy(remainingAmount = event.amount) }
            is AddProductEvent.OnLowStockThresholdChanged ->
                updateState { copy(lowStockThreshold = event.threshold) }
            is AddProductEvent.OnExpirationDateChanged ->
                updateState { copy(expirationDate = event.date) }
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
            is AddProductEvent.OnSaveClick -> save()
            is AddProductEvent.OnBackClick ->
                sendAction(AddProductAction.NavigateBack)
        }
    }

    private fun prefill(event: AddProductEvent.OnPrefill) {
        updateState {
            copy(
                barcode = event.barcode ?: barcode,
                name = event.name ?: name,
                brand = event.brand ?: brand,
                category = event.category ?: category,
                quantity = event.quantity ?: quantity,
                quantityUnit = event.quantityUnit ?: quantityUnit,
                packageAmount = event.packageAmount ?: packageAmount,
                packageUnit = event.packageUnit ?: packageUnit,
                ingredientsText = event.ingredientsText ?: ingredientsText,
                calories = event.calories ?: calories,
                protein = event.protein ?: protein,
                fat = event.fat ?: fat,
                carbs = event.carbs ?: carbs,
                isBarcodePrefilled = event.barcode != null || isBarcodePrefilled
            )
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
                carbs = event.carbs ?: carbs,
                isBarcodePrefilled = true
            )
        }
    }

    private fun save() {
        val state = currentState
        if (state.name.isBlank()) {
            updateState { copy(error = "Введите название продукта") }
            sendAction(AddProductAction.ShowError("Введите название продукта"))
            return
        }
        val qty = state.quantity.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            updateState { copy(error = "Введите корректное количество") }
            sendAction(AddProductAction.ShowError("Введите корректное количество"))
            return
        }

        val expDate = when (val parsed = state.expirationDate.parseOptionalDate("Введите срок годности в формате ГГГГ-ММ-ДД")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val remainingAmount = when (val parsed = state.remainingAmount.parseOptionalNonNegativeDouble("Введите корректный остаток")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val lowStockThreshold = when (val parsed = state.lowStockThreshold.parseOptionalNonNegativeDouble("Введите корректный порог низкого запаса")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val packageAmount = when (val parsed = state.packageAmount.parseOptionalNonNegativeDouble("Введите корректный объем упаковки")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val calories = when (val parsed = state.calories.parseOptionalNonNegativeDouble("Введите корректные калории")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val protein = when (val parsed = state.protein.parseOptionalNonNegativeDouble("Введите корректный белок")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val fat = when (val parsed = state.fat.parseOptionalNonNegativeDouble("Введите корректные жиры")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val carbs = when (val parsed = state.carbs.parseOptionalNonNegativeDouble("Введите корректные углеводы")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }

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
                    brand = state.brand.trim().ifBlank { null },
                    barcode = state.barcode.trim().ifBlank { null },
                    packageAmount = packageAmount,
                    packageUnit = state.packageUnit.takeIf { packageAmount != null },
                    ingredientsText = state.ingredientsText.trim().ifBlank { null },
                    calories = calories,
                    protein = protein,
                    fat = fat,
                    carbs = carbs,
                    remainingAmount = remainingAmount,
                    lowStockThreshold = lowStockThreshold
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

    private fun String.parseOptionalDate(message: String): ParseResult<LocalDate?> {
        if (isBlank()) return ParseResult.Valid(null)
        return runCatching<ParseResult<LocalDate?>> { ParseResult.Valid(LocalDate.parse(this)) }
            .getOrElse {
                updateState { copy(error = message) }
                sendAction(AddProductAction.ShowError(message))
                ParseResult.Invalid
            }
    }

    private fun String.parseOptionalNonNegativeDouble(message: String): ParseResult<Double?> {
        if (isBlank()) return ParseResult.Valid(null)
        val value = toDoubleOrNull()
        if (value != null && value >= 0.0) return ParseResult.Valid(value)
        updateState { copy(error = message) }
        sendAction(AddProductAction.ShowError(message))
        return ParseResult.Invalid
    }

    private sealed interface ParseResult<out T> {
        data class Valid<T>(val value: T) : ParseResult<T>
        data object Invalid : ParseResult<Nothing>
    }
}
