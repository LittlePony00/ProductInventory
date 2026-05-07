package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.MembershipEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaMembershipRepository : JpaRepository<MembershipEntity, UUID> {
    fun findByUserId(userId: UUID): List<MembershipEntity>
    fun findByHouseholdId(householdId: UUID): List<MembershipEntity>
    fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): MembershipEntity?
    fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID)
}
