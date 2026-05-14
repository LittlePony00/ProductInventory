package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val quantityUnit: String,
    val expirationDate: String?,
    val expirationStatus: String,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String,
    val barcode: String? = null,
    val isPendingSync: Boolean = false
)
