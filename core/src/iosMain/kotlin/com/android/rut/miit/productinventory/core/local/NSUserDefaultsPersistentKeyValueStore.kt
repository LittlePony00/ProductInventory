package com.android.rut.miit.productinventory.core.local

import platform.Foundation.NSUserDefaults

class NSUserDefaultsPersistentKeyValueStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : PersistentKeyValueStore {

    override suspend fun read(key: String): String? = defaults.stringForKey(key)

    override suspend fun write(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override suspend fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}
