package com.android.rut.miit.productinventory.feature.products.di

import com.android.rut.miit.productinventory.feature.products.data.IosProductImageFileReader
import com.android.rut.miit.productinventory.feature.products.data.ProductImageFileReader
import com.android.rut.miit.productinventory.feature.realtime.data.KtorSseRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun productPlatformModule(): Module = module {
    single<ProductImageFileReader> { IosProductImageFileReader() }
    single<RealtimeEventSource> { KtorSseRealtimeEventSource(get()) }
}
