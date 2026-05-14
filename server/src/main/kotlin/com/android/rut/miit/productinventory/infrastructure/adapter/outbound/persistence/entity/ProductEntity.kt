package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "products")
class ProductEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String = "",

    var brand: String? = null,

    @Column(name = "barcode")
    var barcode: String? = null,

    @Column(nullable = false)
    var category: String = "OTHER",

    @Column(nullable = false)
    var quantity: Double = 0.0,

    @Column(name = "quantity_unit", nullable = false)
    var quantityUnit: String = "PIECES",

    @Column(name = "package_amount")
    var packageAmount: Double? = null,

    @Column(name = "package_unit")
    var packageUnit: String? = null,

    @Column(name = "ingredients_text", columnDefinition = "TEXT")
    var ingredientsText: String? = null,

    var calories: Double? = null,

    var protein: Double? = null,

    var fat: Double? = null,

    var carbs: Double? = null,

    @Column(name = "purchase_date")
    var purchaseDate: LocalDate? = null,

    @Column(name = "remaining_amount")
    var remainingAmount: Double? = null,

    @Column(name = "low_stock_threshold")
    var lowStockThreshold: Double? = null,

    @Column(name = "expiration_date")
    var expirationDate: LocalDate? = null,

    @Column(name = "household_id", nullable = false)
    var householdId: UUID = UUID.randomUUID(),

    @Column(name = "added_by_user_id", nullable = false)
    var addedByUserId: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
