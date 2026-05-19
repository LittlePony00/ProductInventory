package com.android.rut.miit.productinventory.core.network

import platform.Foundation.NSBundle

actual val apiBaseUrl: String
    get() = (NSBundle.mainBundle.objectForInfoDictionaryKey("API_BASE_URL") as? String)
        ?.takeIf { it.isNotBlank() }
        ?: "http://localhost:8080"
