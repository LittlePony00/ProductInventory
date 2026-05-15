package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaCategoryRepository : JpaRepository<CategoryEntity, UUID> {
    fun findBySystem(system: Boolean): List<CategoryEntity>
    fun findBySystemAndArchived(system: Boolean, archived: Boolean): List<CategoryEntity>
    fun findByHouseholdId(householdId: UUID): List<CategoryEntity>
    fun findByHouseholdIdAndArchived(householdId: UUID, archived: Boolean): List<CategoryEntity>
}
