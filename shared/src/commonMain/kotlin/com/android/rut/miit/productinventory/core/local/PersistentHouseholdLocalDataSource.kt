package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.household.api.models.Household
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentHouseholdLocalDataSource(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) : HouseholdLocalDataSource {

    private val mutex = Mutex()

    override suspend fun getHouseholds(): List<Household> {
        return mutex.withLock { readCache().households.map { it.toDomain() } }
    }

    override suspend fun saveHouseholds(households: List<Household>) {
        mutex.withLock { writeCache(HouseholdCache(households.map { it.toRecord() })) }
    }

    override suspend fun getHouseholdById(id: String): Household? {
        return mutex.withLock { readCache().households.firstOrNull { it.id == id }?.toDomain() }
    }

    private suspend fun readCache(): HouseholdCache {
        val raw = store.read(HOUSEHOLDS_KEY) ?: return HouseholdCache()
        val decoded = runCatching { json.decodeFromString<HouseholdCache>(raw) }
        return decoded.getOrElse {
            store.remove(HOUSEHOLDS_KEY)
            HouseholdCache()
        }
    }

    private suspend fun writeCache(cache: HouseholdCache) {
        store.write(HOUSEHOLDS_KEY, json.encodeToString(cache))
    }

    private fun HouseholdRecord.toDomain() = Household(id = id, name = name, createdAt = createdAt)

    private fun Household.toRecord() = HouseholdRecord(id = id, name = name, createdAt = createdAt)

    private companion object {
        const val HOUSEHOLDS_KEY = "local_households_v1"
    }
}

@Serializable
private data class HouseholdCache(
    val households: List<HouseholdRecord> = emptyList()
)

@Serializable
private data class HouseholdRecord(
    val id: String,
    val name: String,
    val createdAt: String
)
