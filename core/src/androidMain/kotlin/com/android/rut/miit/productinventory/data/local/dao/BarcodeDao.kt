package com.android.rut.miit.productinventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.rut.miit.productinventory.data.local.entity.BarcodeEntity

@Dao
interface BarcodeDao {
    @Query("SELECT * FROM barcode_cache WHERE householdId = :householdId AND barcode = :code LIMIT 1")
    suspend fun getByHouseholdIdAndBarcode(householdId: String, code: String): BarcodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BarcodeEntity)

    @Query("SELECT COUNT(*) > 0 FROM barcode_cache WHERE householdId = :householdId AND barcode = :code")
    suspend fun exists(householdId: String, code: String): Boolean
}
