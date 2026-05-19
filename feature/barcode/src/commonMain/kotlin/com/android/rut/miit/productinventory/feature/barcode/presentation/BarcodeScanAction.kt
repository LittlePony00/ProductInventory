package com.android.rut.miit.productinventory.feature.barcode.presentation

import com.android.rut.miit.productinventory.common.UiAction
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft

sealed class BarcodeScanAction : UiAction {
    data object NavigateBack : BarcodeScanAction()
    data object ProductAdded : BarcodeScanAction()
    data class NavigateToManualEntry(val barcode: String, val householdId: String) : BarcodeScanAction()
    data class NavigateToDraftEntry(
        val draft: BarcodeProductDraft,
        val householdId: String
    ) : BarcodeScanAction()
}
