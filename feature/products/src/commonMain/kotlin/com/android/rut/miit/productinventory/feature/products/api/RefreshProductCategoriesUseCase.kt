package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

class RefreshProductCategoriesUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(householdId: String, includeArchived: Boolean = false): List<ProductCategoryOption> =
        repository.refreshCategories(householdId, includeArchived)
}
