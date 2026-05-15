package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption

class CreateProductCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(householdId: String, name: String): ProductCategoryOption =
        repository.createCategory(householdId, name)
}
