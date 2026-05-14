package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft

interface IBarcodeProductService {
    fun getProductDraft(barcode: String): BarcodeProductDraft
}
