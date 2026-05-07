package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.Member

class GetMembersUseCase(private val repository: HouseholdRepository) {
    suspend operator fun invoke(householdId: String): List<Member> =
        repository.getMembers(householdId)
}
