package com.android.rut.miit.productinventory.feature.products.di

import com.android.rut.miit.productinventory.core.push.AndroidProductLocalReminderRescheduler
import com.android.rut.miit.productinventory.feature.products.data.AndroidProductImageFileReader
import com.android.rut.miit.productinventory.feature.products.data.ProductImageFileReader
import com.android.rut.miit.productinventory.feature.realtime.data.KtorSseRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun productPlatformModule(): Module = module {
    single<ProductImageFileReader> { AndroidProductImageFileReader() }
    single<RealtimeEventSource> { KtorSseRealtimeEventSource(get()) }
    single { AndroidProductLocalReminderRescheduler(get(), get(), get()) }
}
