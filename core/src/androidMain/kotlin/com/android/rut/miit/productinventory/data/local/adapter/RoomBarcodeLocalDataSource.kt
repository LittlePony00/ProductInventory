package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.data.local.dao.BarcodeDao
import com.android.rut.miit.productinventory.data.local.entity.BarcodeEntity

class RoomBarcodeLocalDataSource(
    private val barcodeDao: BarcodeDao
) : BarcodeLocalDataSource {

    override suspend fun getCachedBarcode(householdId: String, code: String): CachedBarcodeProduct? {
        val normalizedBarcode = code.trim()
        return barcodeDao.getByHouseholdIdAndBarcode(householdId, normalizedBarcode)?.toDomain()
            ?: barcodeDao.getByHouseholdIdAndBarcode(LEGACY_HOUSEHOLD_ID, normalizedBarcode)?.toDomain()
    }

    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        barcodeDao.insert(product.copy(barcode = product.barcode.trim()).toEntity())
    }

    override suspend fun isBarcodeKnown(householdId: String, code: String): Boolean {
        val normalizedBarcode = code.trim()
        return barcodeDao.exists(householdId, normalizedBarcode) ||
            barcodeDao.exists(LEGACY_HOUSEHOLD_ID, normalizedBarcode)
    }

    private fun BarcodeEntity.toDomain() = CachedBarcodeProduct(
        householdId = householdId,
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        categoryId = categoryId,
        categoryName = categoryName,
        packageQuantity = packageQuantity,
        packageQuantityUnit = packageQuantityUnit,
        ingredients = ingredients,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
        carbohydratesGrams = carbohydratesGrams,
        source = source,
        updatedAt = updatedAt
    )

    private fun CachedBarcodeProduct.toEntity() = BarcodeEntity(
        householdId = householdId,
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        categoryId = categoryId,
        categoryName = categoryName,
        packageQuantity = packageQuantity,
        packageQuantityUnit = packageQuantityUnit,
        ingredients = ingredients,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
        carbohydratesGrams = carbohydratesGrams,
        source = source,
        updatedAt = updatedAt
    )

    private companion object {
        const val LEGACY_HOUSEHOLD_ID = "__global__"
    }
}
