package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.household.api.models.Household

class InMemoryHouseholdLocalDataSource : HouseholdLocalDataSource {
    private var cache: List<Household> = emptyList()

    override suspend fun getHouseholds(): List<Household> = cache

    override suspend fun saveHouseholds(households: List<Household>) {
        cache = households
    }

    override suspend fun getHouseholdById(id: String): Household? {
        return cache.firstOrNull { it.id == id }
    }
}
