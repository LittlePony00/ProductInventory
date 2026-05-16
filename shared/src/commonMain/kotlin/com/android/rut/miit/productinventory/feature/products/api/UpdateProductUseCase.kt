package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.LocalDate

class UpdateProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        householdId: String,
        productId: String,
        name: String?,
        category: ProductCategory?,
        categoryId: String?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?,
        brand: String? = null,
        barcode: String? = null,
        packageAmount: Double? = null,
        packageUnit: QuantityUnit? = null,
        ingredientsText: String? = null,
        calories: Double? = null,
        protein: Double? = null,
        fat: Double? = null,
        carbs: Double? = null,
        purchaseDate: LocalDate? = null,
        remainingAmount: Double? = null,
        lowStockThreshold: Double? = null
    ): Product =
        repository.updateProduct(
            householdId = householdId,
            productId = productId,
            name = name,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            expirationDate = expirationDate,
            brand = brand,
            barcode = barcode,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold
        )
}
