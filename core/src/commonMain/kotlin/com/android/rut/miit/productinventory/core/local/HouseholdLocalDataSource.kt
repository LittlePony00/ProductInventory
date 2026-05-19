package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.household.api.models.Household

interface HouseholdLocalDataSource {
    suspend fun getHouseholds(): List<Household>
    suspend fun saveHouseholds(households: List<Household>)
    suspend fun getHouseholdById(id: String): Household?
}
