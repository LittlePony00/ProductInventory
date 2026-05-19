package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product

class ConsumeProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(householdId: String, productId: String, amount: Double): Product {
        require(amount > 0.0) { "Consumption amount must be positive" }
        return repository.consumeProduct(householdId, productId, amount)
    }
}
