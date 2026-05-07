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
            is AddProductEvent.OnNameChanged ->
                updateState { copy(name = event.name) }
            is AddProductEvent.OnCategoryChanged ->
                updateState { copy(category = event.category) }
            is AddProductEvent.OnQuantityChanged ->
                updateState { copy(quantity = event.quantity) }
            is AddProductEvent.OnQuantityUnitChanged ->
                updateState { copy(quantityUnit = event.unit) }
            is AddProductEvent.OnExpirationDateChanged ->
                updateState { copy(expirationDate = event.date) }
            is AddProductEvent.OnSaveClick -> save()
            is AddProductEvent.OnBackClick ->
                sendAction(AddProductAction.NavigateBack)
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

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                addProductUseCase(
                    householdId = householdId,
                    name = state.name,
                    category = state.category,
                    quantity = qty,
                    quantityUnit = state.quantityUnit,
                    expirationDate = expDate
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
