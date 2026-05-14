package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import java.util.UUID

interface IBarcodeProductService {
    fun getProductDraft(userId: UUID, householdId: UUID, barcode: String): BarcodeProductDraft
}
