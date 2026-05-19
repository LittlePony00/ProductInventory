package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.local.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localDataSourceModule(): Module = module {
    single<ProductLocalDataSource> { JvmInMemoryProductLocalDataSource() }
    single<HouseholdLocalDataSource> { JvmInMemoryHouseholdLocalDataSource() }
    single<CategoryLocalDataSource> { JvmInMemoryCategoryLocalDataSource() }
    single<BarcodeLocalDataSource> { JvmInMemoryBarcodeLocalDataSource() }
    single<SyncQueue> { JvmInMemorySyncQueue() }
}
