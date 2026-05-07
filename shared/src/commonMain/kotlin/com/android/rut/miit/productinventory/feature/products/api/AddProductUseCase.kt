package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.LocalDate

class AddProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        householdId: String,
        name: String,
        category: ProductCategory,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?
    ): Product {
        return repository.addProduct(householdId, name, category, quantity, quantityUnit, expirationDate)
    }
}
