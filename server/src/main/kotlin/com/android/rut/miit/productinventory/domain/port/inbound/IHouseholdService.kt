package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Household
import com.android.rut.miit.productinventory.domain.model.Membership
import java.util.UUID

interface IHouseholdService {
    fun createHousehold(userId: UUID, name: String): Household
    fun getHousehold(userId: UUID, householdId: UUID): Household
    fun getUserHouseholds(userId: UUID): List<Household>
    fun getMembers(userId: UUID, householdId: UUID): List<Membership>
    fun generateInviteCode(userId: UUID, householdId: UUID): String
    fun joinByInviteCode(userId: UUID, code: String): Household
    fun removeMember(ownerId: UUID, householdId: UUID, memberId: UUID)
    fun leaveHousehold(userId: UUID, householdId: UUID)
}
