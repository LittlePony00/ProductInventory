package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.InviteCodeEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface JpaInviteCodeRepository : JpaRepository<InviteCodeEntity, UUID> {
    fun findByCode(code: String): InviteCodeEntity?
    fun findByHouseholdIdAndUsedFalseAndExpiresAtAfter(householdId: UUID, now: Instant): List<InviteCodeEntity>
}
