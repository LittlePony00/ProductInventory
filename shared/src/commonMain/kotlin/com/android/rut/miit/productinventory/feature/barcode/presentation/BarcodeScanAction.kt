package com.android.rut.miit.productinventory.feature.barcode.presentation

sealed class BarcodeScanAction {
    data object NavigateBack : BarcodeScanAction()
    data object ProductAdded : BarcodeScanAction()
    data class NavigateToManualEntry(val barcode: String, val householdId: String) : BarcodeScanAction()
}
