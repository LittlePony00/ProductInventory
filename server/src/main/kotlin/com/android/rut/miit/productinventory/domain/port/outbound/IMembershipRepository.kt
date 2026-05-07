package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Membership
import java.util.UUID

interface IMembershipRepository {
    fun findByUserId(userId: UUID): List<Membership>
    fun findByHouseholdId(householdId: UUID): List<Membership>
    fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership?
    fun save(membership: Membership): Membership
    fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID)
}
