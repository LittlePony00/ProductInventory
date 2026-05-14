package com.android.rut.miit.productinventory.application.dto.request

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDate

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    val brand: String? = null,

    val barcode: String? = null,

    @field:NotNull(message = "Category is required")
    val category: ProductCategory,

    @field:Positive(message = "Quantity must be positive")
    val quantity: Double,

    @field:NotNull(message = "Quantity unit is required")
    val quantityUnit: QuantityUnit,

    @field:Positive(message = "Package amount must be positive")
    val packageAmount: Double? = null,

    val packageUnit: QuantityUnit? = null,

    val ingredientsText: String? = null,

    @field:PositiveOrZero(message = "Calories must be non-negative")
    val calories: Double? = null,

    @field:PositiveOrZero(message = "Protein must be non-negative")
    val protein: Double? = null,

    @field:PositiveOrZero(message = "Fat must be non-negative")
    val fat: Double? = null,

    @field:PositiveOrZero(message = "Carbs must be non-negative")
    val carbs: Double? = null,

    val purchaseDate: LocalDate? = null,

    @field:PositiveOrZero(message = "Remaining amount must be non-negative")
    val remainingAmount: Double? = null,

    @field:PositiveOrZero(message = "Low stock threshold must be non-negative")
    val lowStockThreshold: Double? = null,

    val expirationDate: LocalDate? = null
)

data class UpdateProductRequest(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val category: ProductCategory? = null,
    @field:Positive(message = "Quantity must be positive")
    val quantity: Double? = null,
    val quantityUnit: QuantityUnit? = null,
    @field:Positive(message = "Package amount must be positive")
    val packageAmount: Double? = null,
    val packageUnit: QuantityUnit? = null,
    val ingredientsText: String? = null,
    @field:PositiveOrZero(message = "Calories must be non-negative")
    val calories: Double? = null,
    @field:PositiveOrZero(message = "Protein must be non-negative")
    val protein: Double? = null,
    @field:PositiveOrZero(message = "Fat must be non-negative")
    val fat: Double? = null,
    @field:PositiveOrZero(message = "Carbs must be non-negative")
    val carbs: Double? = null,
    val purchaseDate: LocalDate? = null,
    @field:PositiveOrZero(message = "Remaining amount must be non-negative")
    val remainingAmount: Double? = null,
    @field:PositiveOrZero(message = "Low stock threshold must be non-negative")
    val lowStockThreshold: Double? = null,
    val expirationDate: LocalDate? = null
)
