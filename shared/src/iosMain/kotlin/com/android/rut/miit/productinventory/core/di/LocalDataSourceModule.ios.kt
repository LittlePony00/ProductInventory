package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.local.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localDataSourceModule(): Module = module {
    single<ProductLocalDataSource> { InMemoryProductLocalDataSource() }
    single<HouseholdLocalDataSource> { InMemoryHouseholdLocalDataSource() }
    single<BarcodeLocalDataSource> { InMemoryBarcodeLocalDataSource() }
    single<SyncQueue> { InMemorySyncQueue() }
}
