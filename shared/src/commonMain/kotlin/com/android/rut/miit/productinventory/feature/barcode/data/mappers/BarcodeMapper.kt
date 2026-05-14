package com.android.rut.miit.productinventory.feature.barcode.data.mappers

import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraftResponseDto
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit

fun BarcodeProductDraftResponseDto.toDomain(): BarcodeProductDraft =
    BarcodeProductDraft(
        barcode = barcode,
        name = name,
        brand = brand,
        packageQuantity = packageQuantity,
        packageQuantityUnit = packageQuantityUnit?.toQuantityUnitOrNull(),
        ingredients = ingredients,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
        carbohydratesGrams = carbohydratesGrams,
        category = category?.toProductCategoryOrNull(),
        source = source.toBarcodeProductSource(),
        confidence = confidence
    )

private fun String.toProductCategoryOrNull(): ProductCategory? =
    runCatching { ProductCategory.valueOf(this) }.getOrNull()

private fun String.toQuantityUnitOrNull(): QuantityUnit? =
    runCatching { QuantityUnit.valueOf(this) }.getOrNull()

private fun String.toBarcodeProductSource(): BarcodeProductSource =
    runCatching { BarcodeProductSource.valueOf(this) }.getOrDefault(BarcodeProductSource.UNKNOWN)
