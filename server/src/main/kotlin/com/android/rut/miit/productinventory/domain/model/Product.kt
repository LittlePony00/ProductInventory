package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Product(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: ProductCategory,
    val categoryId: UUID? = null,
    val categoryName: String? = null,
    val quantity: Quantity,
    val packageQuantity: Quantity? = null,
    val ingredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: LocalDate? = null,
    val remainingAmount: Double = quantity.value,
    val lowStockThreshold: Double? = null,
    val expirationDate: ExpirationDate? = null,
    val householdId: UUID,
    val addedByUserId: UUID,
    val createdAt: Instant = Instant.now()
) {
    init {
        requireNonNegative(remainingAmount, "Remaining amount")
        lowStockThreshold?.let { requireNonNegative(it, "Low stock threshold") }
        calories?.let { requireNonNegative(it, "Calories") }
        protein?.let { requireNonNegative(it, "Protein") }
        fat?.let { requireNonNegative(it, "Fat") }
        carbs?.let { requireNonNegative(it, "Carbs") }
    }
}

private fun requireNonNegative(value: Double, name: String) {
    require(value >= 0) { "$name must be non-negative" }
}
