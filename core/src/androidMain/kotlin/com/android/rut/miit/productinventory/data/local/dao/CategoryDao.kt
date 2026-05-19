package com.android.rut.miit.productinventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.rut.miit.productinventory.data.local.entity.CategoryLocalEntity

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM categories
        WHERE householdId = :householdId AND (:includeArchived OR archived = 0)
        """
    )
    suspend fun getByHouseholdId(householdId: String, includeArchived: Boolean): List<CategoryLocalEntity>

    @Query("DELETE FROM categories WHERE householdId = :householdId")
    suspend fun deleteByHouseholdId(householdId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryLocalEntity)

    @Query("UPDATE categories SET archived = 1 WHERE householdId = :householdId AND id = :categoryId")
    suspend fun archive(householdId: String, categoryId: String)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun delete(categoryId: String)
}
