package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.storage.DataStoreTokenStorage
import com.android.rut.miit.productinventory.core.push.AndroidFirebaseDeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.realtime.data.NoopRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { DataStoreTokenStorage(get()) }
    single<DeviceTokenRegistrar> { AndroidFirebaseDeviceTokenRegistrar(get(), get()) }
    single<RealtimeEventSource> { NoopRealtimeEventSource() }
}
