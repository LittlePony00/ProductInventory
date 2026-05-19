package com.android.rut.miit.productinventory.core.local

data class CachedBarcodeProduct(
    val householdId: String,
    val barcode: String,
    val name: String?,
    val brand: String?,
    val category: String?,
    val categoryId: String?,
    val categoryName: String?,
    val packageQuantity: Double?,
    val packageQuantityUnit: String?,
    val ingredients: String?,
    val imageUrl: String?,
    val localImagePath: String?,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    val carbohydratesGrams: Double?,
    val source: String,
    val updatedAt: Long
)

interface BarcodeLocalDataSource {
    suspend fun getCachedBarcode(householdId: String, code: String): CachedBarcodeProduct?
    suspend fun saveBarcode(product: CachedBarcodeProduct)
    suspend fun isBarcodeKnown(householdId: String, code: String): Boolean
}
