package com.android.rut.miit.productinventory.core.local

import kotlinx.serialization.Serializable

@Serializable
data class PendingSyncAction(
    val id: String,
    val type: SyncActionType,
    val entityId: String,
    val householdId: String,
    val payload: String,
    val createdAt: Long
)

enum class SyncActionType {
    ADD_PRODUCT, UPDATE_PRODUCT, DELETE_PRODUCT
}

interface SyncQueue {
    suspend fun addPendingAction(action: PendingSyncAction)
    suspend fun getPendingActions(): List<PendingSyncAction>
    suspend fun removePendingAction(id: String)
    suspend fun clearAll()
}
