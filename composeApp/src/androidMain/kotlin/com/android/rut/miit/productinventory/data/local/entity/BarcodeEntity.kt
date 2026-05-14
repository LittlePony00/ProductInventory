package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcode_cache")
data class BarcodeEntity(
    @PrimaryKey val barcode: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)
