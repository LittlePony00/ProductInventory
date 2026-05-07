package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.InviteCode

class GenerateInviteCodeUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(householdId: String): InviteCode =
        repository.generateInviteCode(householdId)
}
