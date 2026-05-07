package com.android.rut.miit.productinventory.feature.products.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponseDto(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val quantityUnit: String,
    val expirationDate: String? = null,
    val expirationStatus: String,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String
)

@Serializable
data class CreateProductRequestDto(
    val name: String,
    val category: String,
    val quantity: Double,
    val quantityUnit: String,
    val expirationDate: String? = null
)

@Serializable
data class UpdateProductRequestDto(
    val name: String? = null,
    val category: String? = null,
    val quantity: Double? = null,
    val quantityUnit: String? = null,
    val expirationDate: String? = null
)
