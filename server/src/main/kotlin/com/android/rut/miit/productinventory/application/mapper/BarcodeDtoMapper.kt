package com.android.rut.miit.productinventory.application.mapper

import com.android.rut.miit.productinventory.application.dto.response.barcode.BarcodeProductDraftResponse
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft

fun BarcodeProductDraft.toResponse() = BarcodeProductDraftResponse(
    barcode = barcode,
    name = name,
    brand = brand,
    packageQuantity = packageQuantity?.value,
    packageQuantityUnit = packageQuantity?.unit,
    ingredients = ingredients,
    imageUrl = imageUrl,
    caloriesKcal = nutrition?.caloriesKcal,
    proteinGrams = nutrition?.proteinGrams,
    fatGrams = nutrition?.fatGrams,
    carbohydratesGrams = nutrition?.carbohydratesGrams,
    category = category,
    source = source,
    confidence = confidence
)
