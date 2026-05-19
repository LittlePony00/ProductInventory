package com.android.rut.miit.productinventory.feature.products.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CategoryResponseDto(
    val id: String,
    val householdId: String? = null,
    val code: String? = null,
    val name: String,
    val system: Boolean,
    val archived: Boolean = false,
    val createdAt: String
)

@Serializable
data class CreateCategoryRequestDto(
    val name: String
)

@Serializable
data class UpdateCategoryRequestDto(
    val name: String
)

@Serializable
data class PendingCreateCategoryPayloadDto(
    val name: String,
    val localCategoryId: String
)

@Serializable
data class PendingUpdateCategoryPayloadDto(
    val name: String
)

@Serializable
data object PendingArchiveCategoryPayloadDto
