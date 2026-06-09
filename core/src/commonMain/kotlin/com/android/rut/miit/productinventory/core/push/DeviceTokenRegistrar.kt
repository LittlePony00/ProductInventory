package com.android.rut.miit.productinventory.core.push

interface DeviceTokenRegistrar {
    suspend fun registerCurrentToken()
    suspend fun registerToken(token: String)
}
