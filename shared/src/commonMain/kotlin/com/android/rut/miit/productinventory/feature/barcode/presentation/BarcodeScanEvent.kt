package com.android.rut.miit.productinventory.feature.barcode.presentation

import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft

sealed class BarcodeScanEvent {
    data class OnBarcodeScanned(val code: String) : BarcodeScanEvent()
    data class OnUseDraftClick(val barcode: String) : BarcodeScanEvent()
    data class OnDraftManualEntryClick(val draft: BarcodeProductDraft) : BarcodeScanEvent()
    data object OnRetry : BarcodeScanEvent()
    data object OnBackClick : BarcodeScanEvent()
    data object OnDismissProduct : BarcodeScanEvent()
}
