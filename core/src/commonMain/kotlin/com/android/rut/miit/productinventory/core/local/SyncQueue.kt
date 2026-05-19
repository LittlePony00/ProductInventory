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
    ADD_PRODUCT,
    UPDATE_PRODUCT,
    CONSUME_PRODUCT,
    DELETE_PRODUCT,
    UPLOAD_PRODUCT_IMAGE,
    CREATE_CATEGORY,
    UPDATE_CATEGORY,
    ARCHIVE_CATEGORY
}

interface SyncQueue {
    suspend fun addPendingAction(action: PendingSyncAction)
    suspend fun getPendingActions(): List<PendingSyncAction>
    suspend fun updatePendingAction(action: PendingSyncAction)
    suspend fun removePendingAction(id: String)
    suspend fun clearAll()
}
