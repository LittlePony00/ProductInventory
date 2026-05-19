package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

interface CategoryRepository {
    suspend fun getCategories(householdId: String, includeArchived: Boolean = false): List<ProductCategoryOption>
    suspend fun createCategory(householdId: String, name: String): ProductCategoryOption
    suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption
    suspend fun archiveCategory(householdId: String, categoryId: String)
}
