package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val name: String,
    val category: ProductCategory,
    val quantity: Double,
    val quantityUnit: QuantityUnit,
    val expirationDate: LocalDate?,
    val expirationStatus: ExpirationStatus,
    val householdId: UUID,
    val addedByUserId: UUID,
    val createdAt: Instant
)
