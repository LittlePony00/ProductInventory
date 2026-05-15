package com.android.rut.miit.productinventory.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.data.local.AppDatabase
import com.android.rut.miit.productinventory.data.local.adapter.RoomBarcodeLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomHouseholdLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomProductLocalDataSource
import com.android.rut.miit.productinventory.data.local.adapter.RoomSyncQueue
import com.android.rut.miit.productinventory.feature.realtime.data.KtorSseRealtimeEventSource
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeEventSource
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "product_inventory_db")
            .addMigrations(MIGRATION_1_2)
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
    single<RealtimeEventSource> { KtorSseRealtimeEventSource(get()) }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN brand TEXT")
        db.execSQL("ALTER TABLE products ADD COLUMN packageAmount REAL")
        db.execSQL("ALTER TABLE products ADD COLUMN packageUnit TEXT")
        db.execSQL("ALTER TABLE products ADD COLUMN ingredientsText TEXT")
        db.execSQL("ALTER TABLE products ADD COLUMN calories REAL")
        db.execSQL("ALTER TABLE products ADD COLUMN protein REAL")
        db.execSQL("ALTER TABLE products ADD COLUMN fat REAL")
        db.execSQL("ALTER TABLE products ADD COLUMN carbs REAL")
        db.execSQL("ALTER TABLE products ADD COLUMN purchaseDate TEXT")
        db.execSQL("ALTER TABLE products ADD COLUMN remainingAmount REAL NOT NULL DEFAULT 0")
        db.execSQL("UPDATE products SET remainingAmount = quantity WHERE remainingAmount = 0")
        db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold REAL")
    }
}
