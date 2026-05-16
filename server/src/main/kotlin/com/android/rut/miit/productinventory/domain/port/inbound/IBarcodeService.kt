package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.application.dto.response.ProductResponse

interface IBarcodeService {
    fun lookupAndAddProduct(householdId: String, userId: String, barcode: String): ProductResponse
}
