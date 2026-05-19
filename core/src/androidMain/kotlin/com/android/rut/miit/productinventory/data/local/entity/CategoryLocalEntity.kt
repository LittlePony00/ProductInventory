package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["householdId"])]
)
data class CategoryLocalEntity(
    @PrimaryKey val id: String,
    val householdId: String?,
    val code: String?,
    val name: String,
    val system: Boolean,
    val archived: Boolean,
    val createdAt: String,
    val isPendingSync: Boolean = false
)
