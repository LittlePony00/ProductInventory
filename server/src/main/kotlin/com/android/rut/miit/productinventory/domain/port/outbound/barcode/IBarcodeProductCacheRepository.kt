package com.android.rut.miit.productinventory.domain.port.outbound.barcode

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft

interface IBarcodeProductCacheRepository {
    fun findByBarcode(barcode: String): BarcodeProductDraft?
    fun save(draft: BarcodeProductDraft): BarcodeProductDraft
}
