package com.android.rut.miit.productinventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.rut.miit.productinventory.data.local.entity.HouseholdLocalEntity

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households")
    suspend fun getAll(): List<HouseholdLocalEntity>

    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HouseholdLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(households: List<HouseholdLocalEntity>)

    @Query("DELETE FROM households")
    suspend fun deleteAll()
}
