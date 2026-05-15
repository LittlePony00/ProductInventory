package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import java.time.Instant
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val householdId: UUID?,
    val code: ProductCategory?,
    val name: String,
    val system: Boolean,
    val archived: Boolean,
    val createdAt: Instant
)
