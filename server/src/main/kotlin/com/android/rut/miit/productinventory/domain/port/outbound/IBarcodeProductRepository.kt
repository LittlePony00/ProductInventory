package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.BarcodeProduct

interface IBarcodeProductRepository {
    fun findByBarcode(barcode: String): BarcodeProduct?
    fun save(product: BarcodeProduct): BarcodeProduct
}
