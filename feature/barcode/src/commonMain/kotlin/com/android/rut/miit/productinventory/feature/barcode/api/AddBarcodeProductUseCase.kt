package com.android.rut.miit.productinventory.feature.barcode.api

class AddBarcodeProductUseCase(
    private val repository: BarcodeRepository
) {
    suspend operator fun invoke(householdId: String, barcode: String): BarcodeAddProductResult =
        repository.addBarcodeProduct(householdId, barcode)
}
