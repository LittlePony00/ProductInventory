package com.android.rut.miit.productinventory.feature.products.api.models

import kotlinx.datetime.LocalDate

data class Product(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: ProductCategory,
    val quantity: Double,
    val quantityUnit: QuantityUnit,
    val packageAmount: Double? = null,
    val packageUnit: QuantityUnit? = null,
    val ingredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: LocalDate? = null,
    val remainingAmount: Double = quantity,
    val lowStockThreshold: Double? = null,
    val expirationDate: LocalDate?,
    val expirationStatus: ExpirationStatus,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String
)

enum class ProductCategory {
    DAIRY, MEAT_FISH, VEGETABLES_FRUITS, CEREALS, BEVERAGES, OTHER
}

enum class QuantityUnit {
    GRAMS, MILLILITERS, PIECES
}

enum class ExpirationStatus {
    FRESH, EXPIRING_SOON, EXPIRED, UNKNOWN
}
