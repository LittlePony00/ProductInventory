package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaCategoryRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryRepositoryAdapter(
    private val jpaRepository: JpaCategoryRepository
) : ICategoryRepository {
    override fun findSystemCategories(includeArchived: Boolean): List<Category> =
        if (includeArchived) {
            jpaRepository.findBySystem(true)
        } else {
            jpaRepository.findBySystemAndArchived(true, false)
        }.map { it.toDomain() }

    override fun findByHouseholdId(householdId: UUID, includeArchived: Boolean): List<Category> =
        if (includeArchived) {
            jpaRepository.findByHouseholdId(householdId)
        } else {
            jpaRepository.findByHouseholdIdAndArchived(householdId, false)
        }.map { it.toDomain() }

    override fun findAvailableById(categoryId: UUID, householdId: UUID): Category? =
        jpaRepository.findById(categoryId)
            .orElse(null)
            ?.toDomain()
            ?.takeIf { !it.archived && (it.system || it.householdId == householdId) }

    override fun save(category: Category): Category =
        jpaRepository.save(category.toEntity()).toDomain()
}
