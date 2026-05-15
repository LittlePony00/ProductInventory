package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Category
import java.util.UUID

interface ICategoryService {
    fun getCategories(userId: UUID, householdId: UUID, includeArchived: Boolean = false): List<Category>
    fun createCategory(userId: UUID, householdId: UUID, name: String): Category
    fun updateCategory(userId: UUID, householdId: UUID, categoryId: UUID, name: String): Category
    fun archiveCategory(userId: UUID, householdId: UUID, categoryId: UUID)
}
