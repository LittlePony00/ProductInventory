package com.android.rut.miit.productinventory.core.local

interface PersistentKeyValueStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun remove(key: String)
}
