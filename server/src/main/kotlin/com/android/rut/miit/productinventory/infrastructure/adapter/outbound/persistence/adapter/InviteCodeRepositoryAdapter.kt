package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.InviteCode
import com.android.rut.miit.productinventory.domain.port.outbound.IInviteCodeRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaInviteCodeRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class InviteCodeRepositoryAdapter(
    private val jpaRepository: JpaInviteCodeRepository
) : IInviteCodeRepository {

    override fun findByCode(code: String): InviteCode? =
        jpaRepository.findByCode(code)?.toDomain()

    override fun findActiveByHouseholdId(householdId: UUID): List<InviteCode> =
        jpaRepository.findByHouseholdIdAndUsedFalseAndExpiresAtAfter(householdId, Instant.now())
            .map { it.toDomain() }

    override fun save(inviteCode: InviteCode): InviteCode =
        jpaRepository.save(inviteCode.toEntity()).toDomain()
}
