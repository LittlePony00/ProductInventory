package com.android.rut.miit.productinventory.feature.barcode.presentation

import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.barcode.api.AddBarcodeProductUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.barcode.api.LookupBarcodeUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft

class BarcodeScanViewModel(
    private val lookupBarcodeUseCase: LookupBarcodeUseCase,
    private val addBarcodeProductUseCase: AddBarcodeProductUseCase
) : SharedViewModel<BarcodeScanState, BarcodeScanEvent, BarcodeScanAction>(
    initialState = BarcodeScanState.Scanning
) {

    var householdId: String = ""

    private var lastScannedCode: String? = null

    override suspend fun handleEvent(event: BarcodeScanEvent) {
        when (event) {
            is BarcodeScanEvent.OnBarcodeScanned -> handleBarcode(event.code)
            is BarcodeScanEvent.OnUseDraftClick -> addBarcodeProduct(event.barcode)
            is BarcodeScanEvent.OnDraftManualEntryClick ->
                sendDraftManualEntryAction(event.draft)
            is BarcodeScanEvent.OnRetry -> {
                updateState { BarcodeScanState.Scanning }
                lastScannedCode = null
            }
            is BarcodeScanEvent.OnBackClick -> {
                sendAction(BarcodeScanAction.NavigateBack)
            }
            is BarcodeScanEvent.OnDismissProduct -> {
                updateState { BarcodeScanState.Scanning }
                lastScannedCode = null
            }
        }
    }

    private suspend fun handleBarcode(code: String) {
        if (code == lastScannedCode) return
        lastScannedCode = code
        updateState { BarcodeScanState.Loading }

        when (val result = lookupBarcodeUseCase(householdId, code)) {
            is BarcodeLookupResult.DraftFound -> {
                updateState { BarcodeScanState.DraftFound(result.draft) }
            }
            is BarcodeLookupResult.NeedsManualEntry -> {
                updateState { BarcodeScanState.ManualEntry(result.barcode) }
                sendAction(BarcodeScanAction.NavigateToManualEntry(result.barcode, householdId))
            }
            is BarcodeLookupResult.Error -> {
                updateState { BarcodeScanState.Error(result.message) }
            }
        }
    }

    private suspend fun addBarcodeProduct(barcode: String) {
        updateState { BarcodeScanState.Loading }
        when (val result = addBarcodeProductUseCase(householdId, barcode)) {
            is BarcodeAddProductResult.ProductAdded -> {
                updateState { BarcodeScanState.ProductFound(result.product) }
                sendAction(BarcodeScanAction.ProductAdded)
            }
            is BarcodeAddProductResult.NeedsManualEntry -> {
                updateState { BarcodeScanState.ManualEntry(result.barcode) }
                sendAction(BarcodeScanAction.NavigateToManualEntry(result.barcode, householdId))
            }
            is BarcodeAddProductResult.Error -> {
                updateState { BarcodeScanState.Error(result.message) }
            }
        }
    }

    private fun sendDraftManualEntryAction(draft: BarcodeProductDraft) {
        sendAction(BarcodeScanAction.NavigateToDraftEntry(draft, householdId))
    }
}
