package com.android.rut.miit.productinventory.application.dto.request

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class CreateProductRequest(
    @field:NotBlank(message = "Product name is required")
    val name: String,

    @field:NotNull(message = "Category is required")
    val category: ProductCategory,

    @field:Positive(message = "Quantity must be positive")
    val quantity: Double,

    @field:NotNull(message = "Quantity unit is required")
    val quantityUnit: QuantityUnit,

    val expirationDate: LocalDate? = null
)

data class UpdateProductRequest(
    val name: String? = null,
    val category: ProductCategory? = null,
    val quantity: Double? = null,
    val quantityUnit: QuantityUnit? = null,
    val expirationDate: LocalDate? = null
)
