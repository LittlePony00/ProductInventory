package com.android.rut.miit.productinventory.feature.barcode.api

class ScanBarcodeUseCase(
    private val barcodeRepository: BarcodeRepository
) {
    suspend operator fun invoke(householdId: String, barcode: String): BarcodeResult {
        return barcodeRepository.lookupBarcode(householdId, barcode)
    }
}
