package com.android.rut.miit.productinventory.core.local

import kotlinx.serialization.json.Json

val persistentLocalJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
