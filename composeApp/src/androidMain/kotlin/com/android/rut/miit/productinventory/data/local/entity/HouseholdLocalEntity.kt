package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdLocalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: String
)
