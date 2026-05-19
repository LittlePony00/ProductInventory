package com.android.rut.miit.productinventory.feature.products.api

class DeleteProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(householdId: String, productId: String) {
        repository.deleteProduct(householdId, productId)
    }
}
