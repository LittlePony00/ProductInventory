package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.exception.DomainException
import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.port.inbound.ICategoryService
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryServiceImpl(
    private val categoryRepository: ICategoryRepository,
    private val membershipRepository: IMembershipRepository
) : ICategoryService {

    @Transactional(readOnly = true)
    override fun getCategories(userId: UUID, householdId: UUID, includeArchived: Boolean): List<Category> {
        requireMembership(userId, householdId)
        return categoryRepository.findSystemCategories(includeArchived) +
            categoryRepository.findByHouseholdId(householdId, includeArchived)
    }

    @Transactional
    override fun createCategory(userId: UUID, householdId: UUID, name: String): Category {
        requireMembership(userId, householdId)
        return categoryRepository.save(
            Category(
                householdId = householdId,
                name = name.trim(),
                system = false
            )
        )
    }

    @Transactional
    override fun updateCategory(userId: UUID, householdId: UUID, categoryId: UUID, name: String): Category {
        requireMembership(userId, householdId)
        val category = categoryRepository.findAvailableById(categoryId, householdId)
            ?: throw EntityNotFoundException("Category", categoryId)
        if (category.system) {
            throw DomainException("System categories cannot be edited")
        }
        return categoryRepository.save(category.copy(name = name.trim()))
    }

    @Transactional
    override fun archiveCategory(userId: UUID, householdId: UUID, categoryId: UUID) {
        requireMembership(userId, householdId)
        val category = categoryRepository.findAvailableById(categoryId, householdId)
            ?: throw EntityNotFoundException("Category", categoryId)
        if (category.system) {
            throw DomainException("System categories cannot be archived")
        }
        categoryRepository.save(category.copy(archived = true))
    }

    private fun requireMembership(userId: UUID, householdId: UUID) {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")
    }
}
