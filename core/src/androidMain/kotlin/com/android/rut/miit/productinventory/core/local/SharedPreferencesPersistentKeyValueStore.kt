package com.android.rut.miit.productinventory.core.local

import android.content.Context

class SharedPreferencesPersistentKeyValueStore(
    context: Context
) : PersistentKeyValueStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun read(key: String): String? =
        preferences.getString(key, null)

    override suspend fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override suspend fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "local_key_value_store"
    }
}
