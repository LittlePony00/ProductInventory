package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.Household

class CreateHouseholdUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(name: String): Household = repository.createHousehold(name)
}
