package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

interface CategoryLocalDataSource {
    suspend fun getCategories(householdId: String, includeArchived: Boolean = false): List<ProductCategoryOption>
    suspend fun saveCategories(householdId: String, categories: List<ProductCategoryOption>)
    suspend fun saveCategory(category: ProductCategoryOption)
    suspend fun archiveCategory(householdId: String, categoryId: String)
    suspend fun deleteCategory(categoryId: String)
}
