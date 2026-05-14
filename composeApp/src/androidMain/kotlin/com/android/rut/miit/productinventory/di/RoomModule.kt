package com.android.rut.miit.productinventory.di

import androidx.room.Room
import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.data.local.AppDatabase
import com.android.rut.miit.productinventory.data.local.adapter.RoomBarcodeLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomHouseholdLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomProductLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomSyncQueue
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "product_inventory_db")
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<AppDatabase>().productDao() }
    single { get<AppDatabase>().householdDao() }
    single { get<AppDatabase>().barcodeDao() }
    single { get<AppDatabase>().syncDao() }
    single<ProductLocalDataSource> { RoomProductLocalDataSource(get()) }
    single<HouseholdLocalDataSource> { RoomHouseholdLocalDataSource(get()) }
    single<BarcodeLocalDataSource> { RoomBarcodeLocalDataSource(get()) }
    single<SyncQueue> { RoomSyncQueue(get()) }
}
