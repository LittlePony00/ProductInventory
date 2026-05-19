package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double,
    val lowStockThreshold: Double? = null,
    val expirationDate: String?,
    val expirationStatus: String,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String,
    val isPendingSync: Boolean = false
)
