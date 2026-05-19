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
import org.koin.dsl.module

val roomModule = module {
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "product_inventory_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("products", "categoryId", "TEXT")
        db.addColumnIfMissing("products", "categoryName", "TEXT")
        db.addColumnIfMissing("products", "isPendingSync", "INTEGER NOT NULL DEFAULT 0")
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing("products", "imageUrl", "TEXT")
        db.addColumnIfMissing("products", "localImagePath", "TEXT")
    }
}

private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
    val cursor = query("PRAGMA table_info($table)")
    cursor.use {
        val nameIndex = it.getColumnIndex("name")
        while (it.moveToNext()) {
            if (it.getString(nameIndex) == column) return
        }
    }
    execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
}
