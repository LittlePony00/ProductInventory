package com.android.rut.miit.productinventory.feature.products.data.mappers

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.data.models.CategoryResponseDto

fun CategoryResponseDto.toDomain() = ProductCategoryOption(
    id = id,
    householdId = householdId,
    code = code?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
    name = name,
    system = system,
    archived = archived,
    createdAt = createdAt
)
