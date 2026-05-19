package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product

class RefreshProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(householdId: String, categoryId: String? = null): List<Product> {
        return repository.refreshProducts(householdId, categoryId)
    }
}
