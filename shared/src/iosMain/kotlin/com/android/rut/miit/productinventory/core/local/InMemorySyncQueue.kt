package com.android.rut.miit.productinventory.core.local

class InMemorySyncQueue : SyncQueue {
    private val queue = mutableListOf<PendingSyncAction>()

    override suspend fun addPendingAction(action: PendingSyncAction) {
        queue.add(action)
    }

    override suspend fun getPendingActions(): List<PendingSyncAction> = queue.toList()

    override suspend fun removePendingAction(id: String) {
        queue.removeAll { it.id == id }
    }

    override suspend fun clearAll() {
        queue.clear()
    }
}
