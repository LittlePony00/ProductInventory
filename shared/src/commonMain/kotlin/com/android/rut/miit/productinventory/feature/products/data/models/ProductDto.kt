package com.android.rut.miit.productinventory.feature.products.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponseDto(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: String,
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
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
    val quantity: Double,
    val quantityUnit: String,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
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
    val quantity: Double? = null,
    val quantityUnit: String? = null,
    val packageAmount: Double? = null,
    val packageUnit: String? = null,
    val ingredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: String? = null,
    val remainingAmount: Double? = null,
    val lowStockThreshold: Double? = null,
    val expirationDate: String? = null
)
