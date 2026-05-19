package com.android.rut.miit.productinventory.feature.products.di

import com.android.rut.miit.productinventory.feature.products.data.JvmProductImageFileReader
import com.android.rut.miit.productinventory.feature.products.data.ProductImageFileReader
import com.android.rut.miit.productinventory.feature.realtime.data.NoopRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun productPlatformModule(): Module = module {
    single<ProductImageFileReader> { JvmProductImageFileReader() }
    single<RealtimeEventSource> { NoopRealtimeEventSource() }
}
