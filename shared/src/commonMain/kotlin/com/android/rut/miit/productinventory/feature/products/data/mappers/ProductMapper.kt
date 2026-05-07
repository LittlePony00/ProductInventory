package com.android.rut.miit.productinventory.feature.products.data.mappers

import com.android.rut.miit.productinventory.feature.products.api.models.*
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import kotlinx.datetime.LocalDate

fun ProductResponseDto.toDomain() = Product(
    id = id,
    name = name,
    category = try { ProductCategory.valueOf(category) } catch (_: Exception) { ProductCategory.OTHER },
    quantity = quantity,
    quantityUnit = try { QuantityUnit.valueOf(quantityUnit) } catch (_: Exception) { QuantityUnit.PIECES },
    expirationDate = expirationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    expirationStatus = try { ExpirationStatus.valueOf(expirationStatus) } catch (_: Exception) { ExpirationStatus.UNKNOWN },
    householdId = householdId,
    addedByUserId = addedByUserId,
    createdAt = createdAt
)
