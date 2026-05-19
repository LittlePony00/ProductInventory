package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.Household

class GetHouseholdsUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(): List<Household> = repository.getCachedHouseholds()
}
