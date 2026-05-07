package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface JpaProductRepository : JpaRepository<ProductEntity, UUID> {
    fun findByHouseholdId(householdId: UUID): List<ProductEntity>
    fun findByHouseholdIdAndExpirationDateBefore(householdId: UUID, date: LocalDate): List<ProductEntity>
}
