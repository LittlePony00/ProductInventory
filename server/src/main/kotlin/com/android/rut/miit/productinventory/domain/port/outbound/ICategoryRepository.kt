package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Category
import java.util.UUID

interface ICategoryRepository {
    fun findSystemCategories(includeArchived: Boolean = false): List<Category>
    fun findByHouseholdId(householdId: UUID, includeArchived: Boolean = false): List<Category>
    fun findAvailableById(categoryId: UUID, householdId: UUID): Category?
    fun save(category: Category): Category
}
