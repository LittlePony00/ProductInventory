package com.android.rut.miit.productinventory.application.dto.response.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource

data class BarcodeProductDraftResponse(
    val barcode: String,
    val name: String?,
    val brand: String?,
    val packageQuantity: Double?,
    val packageQuantityUnit: QuantityUnit?,
    val ingredients: String?,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    val carbohydratesGrams: Double?,
    val category: ProductCategory?,
    val source: BarcodeProductSource,
    val confidence: Double
)
