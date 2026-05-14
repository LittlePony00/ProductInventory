package com.android.rut.miit.productinventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.rut.miit.productinventory.data.local.entity.PendingSyncActionEntity

@Dao
interface SyncDao {
    @Query("SELECT * FROM pending_sync_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingSyncActionEntity)

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_sync_actions")
    suspend fun deleteAll()
}
