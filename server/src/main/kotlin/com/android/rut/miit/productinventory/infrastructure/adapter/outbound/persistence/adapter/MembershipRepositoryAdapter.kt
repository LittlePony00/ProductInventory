package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaMembershipRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MembershipRepositoryAdapter(
    private val jpaRepository: JpaMembershipRepository
) : IMembershipRepository {

    override fun findByUserId(userId: UUID): List<Membership> =
        jpaRepository.findByUserId(userId).map { it.toDomain() }

    override fun findByHouseholdId(householdId: UUID): List<Membership> =
        jpaRepository.findByHouseholdId(householdId).map { it.toDomain() }

    override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
        jpaRepository.findByUserIdAndHouseholdId(userId, householdId)?.toDomain()

    override fun save(membership: Membership): Membership =
        jpaRepository.save(membership.toEntity()).toDomain()

    override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) =
        jpaRepository.deleteByUserIdAndHouseholdId(userId, householdId)
}
