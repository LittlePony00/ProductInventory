package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.push.NoOpDeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.storage.InMemoryTokenStorage
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { InMemoryTokenStorage() }
    single<DeviceTokenRegistrar> { NoOpDeviceTokenRegistrar() }
}
