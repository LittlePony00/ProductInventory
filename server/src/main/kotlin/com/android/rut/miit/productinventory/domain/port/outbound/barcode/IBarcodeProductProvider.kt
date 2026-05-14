package com.android.rut.miit.productinventory.domain.port.outbound.barcode

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import java.util.UUID

data class BarcodeLookupContext(
    val userId: UUID,
    val householdId: UUID,
    val barcode: String
)

interface IBarcodeProductProvider {
    val order: BarcodeProductProviderOrder
    fun findDraft(context: BarcodeLookupContext): BarcodeProductDraft?
}

enum class BarcodeProductProviderOrder {
    OPEN_FOOD_FACTS,
    GS1,
    LOCAL_DATABASE
}
