package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.CategoryLocalDataSource
import com.android.rut.miit.productinventory.data.local.dao.CategoryDao
import com.android.rut.miit.productinventory.data.local.entity.CategoryLocalEntity
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

class RoomCategoryLocalDataSource(
    private val categoryDao: CategoryDao
) : CategoryLocalDataSource {

    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
        categoryDao.getByHouseholdId(householdId, includeArchived).map { it.toDomain() }

    override suspend fun saveCategories(householdId: String, categories: List<ProductCategoryOption>) {
        categoryDao.deleteByHouseholdId(householdId)
        categoryDao.insertAll(categories.map { it.toEntity(householdId) })
    }

    override suspend fun saveCategory(category: ProductCategoryOption) {
        val householdId = category.householdId ?: return
        categoryDao.insert(category.toEntity(householdId))
    }

    override suspend fun archiveCategory(householdId: String, categoryId: String) {
        categoryDao.archive(householdId, categoryId)
    }

    override suspend fun deleteCategory(categoryId: String) {
        categoryDao.delete(categoryId)
    }

    private fun CategoryLocalEntity.toDomain() = ProductCategoryOption(
        id = id,
        householdId = householdId,
        code = code?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
        name = name,
        system = system,
        archived = archived,
        createdAt = createdAt
    )

    private fun ProductCategoryOption.toEntity(fallbackHouseholdId: String) = CategoryLocalEntity(
        id = id,
        householdId = householdId ?: fallbackHouseholdId,
        code = code?.name,
        name = name,
        system = system,
        archived = archived,
        createdAt = createdAt,
        isPendingSync = false
    )
}
