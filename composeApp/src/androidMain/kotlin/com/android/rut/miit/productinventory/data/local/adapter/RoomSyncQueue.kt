package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.data.local.dao.SyncDao
import com.android.rut.miit.productinventory.data.local.entity.PendingSyncActionEntity

class RoomSyncQueue(
    private val syncDao: SyncDao
) : SyncQueue {

    override suspend fun addPendingAction(action: PendingSyncAction) {
        syncDao.insert(action.toEntity())
    }

    override suspend fun getPendingActions(): List<PendingSyncAction> {
        return syncDao.getAll().map { it.toDomain() }
    }

    override suspend fun updatePendingAction(action: PendingSyncAction) {
        syncDao.insert(action.toEntity())
    }

    override suspend fun removePendingAction(id: String) {
        syncDao.deleteById(id)
    }

    override suspend fun clearAll() {
        syncDao.deleteAll()
    }

    private fun PendingSyncActionEntity.toDomain() = PendingSyncAction(
        id = id,
        type = runCatching { SyncActionType.valueOf(type) }.getOrElse { SyncActionType.ADD_PRODUCT },
        entityId = entityId,
        householdId = householdId,
        payload = payload,
        createdAt = createdAt
    )

    private fun PendingSyncAction.toEntity() = PendingSyncActionEntity(
        id = id,
        type = type.name,
        entityId = entityId,
        householdId = householdId,
        payload = payload,
        createdAt = createdAt
    )
}
