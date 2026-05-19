package com.android.rut.miit.productinventory.feature.products.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponseDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val imageUrl: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double = quantity,
    val lowStockThreshold: Double? = null,
    val expirationDate: String? = null,
    val expirationStatus: String,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String
)

@Serializable
data class CreateProductRequestDto(
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String,
    val categoryId: String? = null,
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val imageUrl: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double? = null,
    val lowStockThreshold: Double? = null,
    val expirationDate: String? = null
)

@Serializable
data class UpdateProductRequestDto(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val categoryId: String? = null,
    val quantity: Double? = null,
    val quantityUnit: String? = null,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val imageUrl: String? = null,
    val clearImage: Boolean = false,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double? = null,
    val lowStockThreshold: Double? = null,
    val expirationDate: String? = null
)

@Serializable
data class PendingCreateProductPayloadDto(
    val request: CreateProductRequestDto,
    val localImagePath: String? = null
)

@Serializable
data class PendingUpdateProductPayloadDto(
    val request: UpdateProductRequestDto,
    val localImagePath: String? = null
)

@Serializable
data class PendingUploadProductImagePayloadDto(
    val localImagePath: String
)

@Serializable
data class ConsumeProductRequestDto(
    val amount: Double
)

@Serializable
data class ProductEnrichmentSuggestionRequestDto(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val ingredientsText: String? = null
)

@Serializable
data class ProductEnrichmentSuggestionResponseDto(
    val categoryId: String,
    val category: String,
    val categoryName: String,
    val confidence: Double,
    val source: String,
    val suggestedName: String? = null,
    val suggestedBrand: String? = null,
    val suggestedIngredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
)
