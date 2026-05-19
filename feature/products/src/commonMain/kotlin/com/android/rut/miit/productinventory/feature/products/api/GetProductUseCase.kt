package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product

class GetProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(householdId: String, productId: String): Product =
        repository.getProduct(householdId, productId)
}
