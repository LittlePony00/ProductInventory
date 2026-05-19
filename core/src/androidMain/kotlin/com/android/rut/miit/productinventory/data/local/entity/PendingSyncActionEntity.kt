package com.android.rut.miit.productinventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncActionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val entityId: String,
    val householdId: String,
    val payload: String,
    val createdAt: Long
)
