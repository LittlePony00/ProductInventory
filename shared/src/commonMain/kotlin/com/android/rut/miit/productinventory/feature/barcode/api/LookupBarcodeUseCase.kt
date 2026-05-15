package com.android.rut.miit.productinventory.feature.barcode.api

class LookupBarcodeUseCase(
    private val repository: BarcodeRepository
) {
    suspend operator fun invoke(householdId: String, barcode: String): BarcodeLookupResult =
        repository.lookupBarcode(householdId, barcode)
}
