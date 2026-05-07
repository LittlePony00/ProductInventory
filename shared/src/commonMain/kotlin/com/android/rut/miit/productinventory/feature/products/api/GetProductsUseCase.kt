package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product

class GetProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(householdId: String): List<Product> {
        return repository.getProducts(householdId)
    }
}
