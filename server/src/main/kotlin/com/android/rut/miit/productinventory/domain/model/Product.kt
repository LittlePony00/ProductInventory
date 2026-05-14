package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Product(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val barcode: String? = null,
    val brand: String? = null,
    val ingredients: String? = null,
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
    val carbohydratesGrams: Double? = null,
    val category: ProductCategory,
    val quantity: Quantity,
    val expirationDate: ExpirationDate? = null,
    val householdId: UUID,
    val addedByUserId: UUID,
    val createdAt: Instant = Instant.now()
)
