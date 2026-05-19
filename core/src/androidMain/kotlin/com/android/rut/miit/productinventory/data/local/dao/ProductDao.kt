package com.android.rut.miit.productinventory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.rut.miit.productinventory.data.local.entity.ProductLocalEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE householdId = :householdId")
    suspend fun getByHouseholdId(householdId: String): List<ProductLocalEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductLocalEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProductLocalEntity?

    @Query("SELECT * FROM products WHERE householdId = :householdId AND id = :id LIMIT 1")
    suspend fun getByHouseholdIdAndId(householdId: String, id: String): ProductLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductLocalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductLocalEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM products WHERE householdId = :householdId")
    suspend fun deleteAllByHouseholdId(householdId: String)
}
