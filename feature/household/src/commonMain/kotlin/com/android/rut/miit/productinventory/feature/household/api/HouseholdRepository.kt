package com.android.rut.miit.productinventory.feature.household.api

import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.api.models.InviteCode
import com.android.rut.miit.productinventory.feature.household.api.models.Member

interface HouseholdRepository {
    suspend fun getCachedHouseholds(): List<Household> = getMyHouseholds()
    suspend fun getMyHouseholds(): List<Household>
    suspend fun refreshMyHouseholds(): List<Household> = getMyHouseholds()
    suspend fun getHousehold(householdId: String): Household
    suspend fun createHousehold(name: String): Household
    suspend fun getMembers(householdId: String): List<Member>
    suspend fun generateInviteCode(householdId: String): InviteCode
    suspend fun joinByInviteCode(inviteCode: String): Household
    suspend fun removeMember(householdId: String, memberId: String)
    suspend fun leaveHousehold(householdId: String)
}
