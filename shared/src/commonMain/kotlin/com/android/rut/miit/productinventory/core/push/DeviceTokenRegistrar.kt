package com.android.rut.miit.productinventory.core.push

interface DeviceTokenRegistrar {
    suspend fun registerCurrentToken()
    suspend fun registerToken(token: String)
}

class NoOpDeviceTokenRegistrar : DeviceTokenRegistrar {
    override suspend fun registerCurrentToken() = Unit
    override suspend fun registerToken(token: String) = Unit
}
