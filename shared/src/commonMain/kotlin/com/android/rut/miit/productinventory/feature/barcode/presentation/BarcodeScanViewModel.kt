package com.android.rut.miit.productinventory.feature.barcode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.feature.barcode.api.AddBarcodeProductUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.barcode.api.LookupBarcodeUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BarcodeScanViewModel(
    private val lookupBarcodeUseCase: LookupBarcodeUseCase,
    private val addBarcodeProductUseCase: AddBarcodeProductUseCase
) : ViewModel() {

    var householdId: String = ""

    private val _viewState = MutableStateFlow<BarcodeScanState>(BarcodeScanState.Scanning)
    val viewState: StateFlow<BarcodeScanState> = _viewState

    private val _viewAction = MutableSharedFlow<BarcodeScanAction>()
    val viewAction: SharedFlow<BarcodeScanAction> = _viewAction

    private var lastScannedCode: String? = null

    fun onEvent(event: BarcodeScanEvent) {
        when (event) {
            is BarcodeScanEvent.OnBarcodeScanned -> handleBarcode(event.code)
            is BarcodeScanEvent.OnUseDraftClick -> addBarcodeProduct(event.barcode)
            is BarcodeScanEvent.OnDraftManualEntryClick ->
                sendDraftManualEntryAction(event.draft)
            is BarcodeScanEvent.OnRetry -> {
                _viewState.value = BarcodeScanState.Scanning
                lastScannedCode = null
            }
            is BarcodeScanEvent.OnBackClick -> {
                viewModelScope.launch { _viewAction.emit(BarcodeScanAction.NavigateBack) }
            }
            is BarcodeScanEvent.OnDismissProduct -> {
                _viewState.value = BarcodeScanState.Scanning
                lastScannedCode = null
            }
        }
    }

    private fun handleBarcode(code: String) {
        if (code == lastScannedCode) return
        lastScannedCode = code
        _viewState.value = BarcodeScanState.Loading

        viewModelScope.launch {
            when (val result = lookupBarcodeUseCase(code)) {
                is BarcodeLookupResult.DraftFound -> {
                    _viewState.value = BarcodeScanState.DraftFound(result.draft)
                }
                is BarcodeLookupResult.NeedsManualEntry -> {
                    _viewState.value = BarcodeScanState.ManualEntry(result.barcode)
                    _viewAction.emit(
                        BarcodeScanAction.NavigateToManualEntry(result.barcode, householdId)
                    )
                }
                is BarcodeLookupResult.Error -> {
                    _viewState.value = BarcodeScanState.Error(result.message)
                }
            }
        }
    }

    private fun addBarcodeProduct(barcode: String) {
        viewModelScope.launch {
            _viewState.value = BarcodeScanState.Loading
            when (val result = addBarcodeProductUseCase(householdId, barcode)) {
                is BarcodeAddProductResult.ProductAdded -> {
                    _viewState.value = BarcodeScanState.ProductFound(result.product)
                    _viewAction.emit(BarcodeScanAction.ProductAdded)
                }
                is BarcodeAddProductResult.NeedsManualEntry -> {
                    _viewState.value = BarcodeScanState.ManualEntry(result.barcode)
                    _viewAction.emit(
                        BarcodeScanAction.NavigateToManualEntry(result.barcode, householdId)
                    )
                }
                is BarcodeAddProductResult.Error -> {
                    _viewState.value = BarcodeScanState.Error(result.message)
                }
            }
        }
    }

    private fun sendDraftManualEntryAction(draft: BarcodeProductDraft) {
        viewModelScope.launch {
            _viewAction.emit(BarcodeScanAction.NavigateToDraftEntry(draft, householdId))
        }
    }
}
