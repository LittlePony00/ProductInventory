package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.push.IosFirebaseDeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.storage.NSUserDefaultsTokenStorage
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.realtime.data.KtorSseRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { NSUserDefaultsTokenStorage() }
    single<DeviceTokenRegistrar> { IosFirebaseDeviceTokenRegistrar(get(), get()) }
    single<RealtimeEventSource> { KtorSseRealtimeEventSource(get()) }
}
