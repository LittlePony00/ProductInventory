package com.android.rut.miit.productinventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.android.rut.miit.productinventory.data.local.dao.BarcodeDao
import com.android.rut.miit.productinventory.data.local.dao.HouseholdDao
import com.android.rut.miit.productinventory.data.local.dao.ProductDao
import com.android.rut.miit.productinventory.data.local.dao.SyncDao
import com.android.rut.miit.productinventory.data.local.entity.BarcodeEntity
import com.android.rut.miit.productinventory.data.local.entity.HouseholdLocalEntity
import com.android.rut.miit.productinventory.data.local.entity.PendingSyncActionEntity
import com.android.rut.miit.productinventory.data.local.entity.ProductLocalEntity

@Database(
    entities = [
        ProductLocalEntity::class,
        HouseholdLocalEntity::class,
        BarcodeEntity::class,
        PendingSyncActionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun householdDao(): HouseholdDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun syncDao(): SyncDao
}
