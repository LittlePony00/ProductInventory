package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity

@Entity(tableName = "barcode_cache", primaryKeys = ["householdId", "barcode"])
data class BarcodeEntity(
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
