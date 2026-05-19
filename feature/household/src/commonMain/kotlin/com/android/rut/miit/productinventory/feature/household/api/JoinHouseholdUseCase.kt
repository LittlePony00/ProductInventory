package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.Household

class JoinHouseholdUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(inviteCode: String): Household = repository.joinByInviteCode(inviteCode)
}
