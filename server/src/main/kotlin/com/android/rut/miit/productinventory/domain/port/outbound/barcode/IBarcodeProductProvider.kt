package com.android.rut.miit.productinventory.domain.port.outbound.barcode

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft

interface IBarcodeProductProvider {
    val order: BarcodeProductProviderOrder
    fun findDraft(barcode: String): BarcodeProductDraft?
}

enum class BarcodeProductProviderOrder {
    OPEN_FOOD_FACTS,
    GS1,
    LOCAL_DATABASE
}
