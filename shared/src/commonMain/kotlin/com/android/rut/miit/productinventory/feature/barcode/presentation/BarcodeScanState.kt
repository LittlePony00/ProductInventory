package com.android.rut.miit.productinventory.feature.barcode.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.products.api.models.Product

sealed class BarcodeScanState : UiState {
    data object Scanning : BarcodeScanState()
    data object Loading : BarcodeScanState()
    data class DraftFound(val draft: BarcodeProductDraft) : BarcodeScanState()
    data class ProductFound(val product: Product) : BarcodeScanState()
    data class ManualEntry(val barcode: String) : BarcodeScanState()
    data class Error(val message: String) : BarcodeScanState()
}
