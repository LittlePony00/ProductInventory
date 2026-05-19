package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

class UpdateProductCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(householdId: String, categoryId: String, name: String): ProductCategoryOption =
        repository.updateCategory(householdId, categoryId, name)
}
