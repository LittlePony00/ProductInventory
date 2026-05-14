package com.android.rut.miit.productinventory.feature.barcode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeResult
import com.android.rut.miit.productinventory.feature.barcode.api.ScanBarcodeUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BarcodeScanViewModel(
    private val scanBarcodeUseCase: ScanBarcodeUseCase
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
            when (val result = scanBarcodeUseCase(householdId, code)) {
                is BarcodeResult.ProductFound -> {
                    _viewState.value = BarcodeScanState.ProductFound(result.product)
                    _viewAction.emit(BarcodeScanAction.ProductAdded)
                }
                is BarcodeResult.NeedsManualEntry -> {
                    _viewState.value = BarcodeScanState.ManualEntry(result.barcode)
                    _viewAction.emit(
                        BarcodeScanAction.NavigateToManualEntry(result.barcode, householdId)
                    )
                }
                is BarcodeResult.Error -> {
                    _viewState.value = BarcodeScanState.Error(result.message)
                }
            }
        }
    }
}
