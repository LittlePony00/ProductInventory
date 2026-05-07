package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Household
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaHouseholdRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HouseholdRepositoryAdapter(
    private val jpaRepository: JpaHouseholdRepository
) : IHouseholdRepository {

    override fun findById(id: UUID): Household? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(household: Household): Household =
        jpaRepository.save(household.toEntity()).toDomain()

    override fun deleteById(id: UUID) =
        jpaRepository.deleteById(id)
}
