package com.android.rut.miit.productinventory.feature.barcode.presentation

sealed class BarcodeScanEvent {
    data class OnBarcodeScanned(val code: String) : BarcodeScanEvent()
    data object OnRetry : BarcodeScanEvent()
    data object OnBackClick : BarcodeScanEvent()
    data object OnDismissProduct : BarcodeScanEvent()
}
