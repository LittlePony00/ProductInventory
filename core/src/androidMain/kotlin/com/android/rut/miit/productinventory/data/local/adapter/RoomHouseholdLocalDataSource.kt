package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.data.local.dao.HouseholdDao
import com.android.rut.miit.productinventory.data.local.entity.HouseholdLocalEntity
import com.android.rut.miit.productinventory.feature.household.api.models.Household

class RoomHouseholdLocalDataSource(
    private val householdDao: HouseholdDao
) : HouseholdLocalDataSource {

    override suspend fun getHouseholds(): List<Household> {
        return householdDao.getAll().map { it.toDomain() }
    }

    override suspend fun saveHouseholds(households: List<Household>) {
        householdDao.deleteAll()
        householdDao.insertAll(households.map { it.toEntity() })
    }

    override suspend fun getHouseholdById(id: String): Household? {
        return householdDao.getById(id)?.toDomain()
    }

    private fun HouseholdLocalEntity.toDomain() = Household(
        id = id,
        name = name,
        createdAt = createdAt
    )

    private fun Household.toEntity() = HouseholdLocalEntity(
        id = id,
        name = name,
        createdAt = createdAt
    )
}
