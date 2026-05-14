package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.OpenFoodFactsProduct

interface IProductInfoProvider {
    fun lookupByBarcode(barcode: String): OpenFoodFactsProduct?
}
