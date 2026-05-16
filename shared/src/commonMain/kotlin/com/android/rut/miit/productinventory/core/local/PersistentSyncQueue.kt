package com.android.rut.miit.productinventory.core.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentSyncQueue(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) : SyncQueue {

    private val mutex = Mutex()

    override suspend fun addPendingAction(action: PendingSyncAction) {
        mutex.withLock {
            val cache = readCache()
            writeCache(cache.copy(actions = cache.actions.filterNot { it.id == action.id } + action))
        }
    }

    override suspend fun getPendingActions(): List<PendingSyncAction> {
        return mutex.withLock { readCache().actions }
    }

    override suspend fun updatePendingAction(action: PendingSyncAction) {
        mutex.withLock {
            val cache = readCache()
            writeCache(cache.copy(actions = cache.actions.map { if (it.id == action.id) action else it }))
        }
    }

    override suspend fun removePendingAction(id: String) {
        mutex.withLock {
            writeCache(readCache().let { it.copy(actions = it.actions.filterNot { action -> action.id == id }) })
        }
    }

    override suspend fun clearAll() {
        mutex.withLock { store.remove(SYNC_QUEUE_KEY) }
    }

    private suspend fun readCache(): SyncQueueCache {
        val raw = store.read(SYNC_QUEUE_KEY) ?: return SyncQueueCache()
        val decoded = runCatching { json.decodeFromString<SyncQueueCache>(raw) }
        return decoded.getOrElse {
            store.remove(SYNC_QUEUE_KEY)
            SyncQueueCache()
        }
    }

    private suspend fun writeCache(cache: SyncQueueCache) {
        store.write(SYNC_QUEUE_KEY, json.encodeToString(cache))
    }

    private companion object {
        const val SYNC_QUEUE_KEY = "local_sync_queue_v1"
    }
}

@Serializable
private data class SyncQueueCache(
    val actions: List<PendingSyncAction> = emptyList()
)
