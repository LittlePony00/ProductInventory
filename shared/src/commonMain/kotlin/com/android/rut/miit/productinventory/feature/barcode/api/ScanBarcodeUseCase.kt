package com.android.rut.miit.productinventory.feature.barcode.api

class ScanBarcodeUseCase(
    private val barcodeRepository: BarcodeRepository
) {
    suspend operator fun invoke(householdId: String, barcode: String): BarcodeResult {
        return when (val result = barcodeRepository.lookupBarcode(barcode)) {
            is BarcodeLookupResult.DraftFound -> BarcodeResult.DraftFound(result.draft)
            is BarcodeLookupResult.NeedsManualEntry -> BarcodeResult.NeedsManualEntry(result.barcode)
            is BarcodeLookupResult.Error -> BarcodeResult.Error(result.message)
        }
    }
}
