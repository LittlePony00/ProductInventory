package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.local.*
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localDataSourceModule(): Module = module {
    single<PersistentKeyValueStore> { NSUserDefaultsPersistentKeyValueStore() }
    single<ProductLocalDataSource> { PersistentProductLocalDataSource(get()) }
    single<HouseholdLocalDataSource> { PersistentHouseholdLocalDataSource(get()) }
    single<CategoryLocalDataSource> { PersistentCategoryLocalDataSource(get()) }
    single<BarcodeLocalDataSource> { PersistentBarcodeLocalDataSource(get()) }
    single<SyncQueue> { PersistentSyncQueue(get()) }
}
