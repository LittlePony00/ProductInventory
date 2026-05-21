package com.android.rut.miit.productinventory.feature.profile.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponseDto(
    val id: String,
    val email: String,
    val name: String
)

@Serializable
data class UpdateProfileRequestDto(
    val name: String
)

@Serializable
data class FoodPreferencesResponseDto(
    val preferredCuisines: Set<String> = emptySet(),
    val preferredProducts: Set<String> = emptySet(),
    val dislikedIngredients: Set<String> = emptySet(),
    val avoidedProducts: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    val dietaryRestrictions: Set<String> = emptySet(),
    val preferredProductIds: Set<String> = emptySet(),
    val avoidedProductIds: Set<String> = emptySet(),
    val preferredCategoryIds: Set<String> = emptySet(),
    val avoidedCategoryIds: Set<String> = emptySet(),
    val maxCookingTimeMinutes: Int? = null,
    val preferredDifficulty: String? = null,
    val servings: Int? = null
)

@Serializable
data class UpdateFoodPreferencesRequestDto(
    val preferredCuisines: Set<String> = emptySet(),
    val preferredProducts: Set<String> = emptySet(),
    val dislikedIngredients: Set<String> = emptySet(),
    val avoidedProducts: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    val dietaryRestrictions: Set<String> = emptySet(),
    val preferredProductIds: Set<String> = emptySet(),
    val avoidedProductIds: Set<String> = emptySet(),
    val preferredCategoryIds: Set<String> = emptySet(),
    val avoidedCategoryIds: Set<String> = emptySet(),
    val maxCookingTimeMinutes: Int? = null,
    val preferredDifficulty: String? = null,
    val servings: Int? = null
)

@Serializable
data class FoodPreferencesOptionsResponseDto(
    val products: List<FoodPreferenceProductOptionDto> = emptyList(),
    val categories: List<FoodPreferenceCategoryOptionDto> = emptyList()
)

@Serializable
data class FoodPreferenceProductOptionDto(
    val id: String,
    val name: String,
    val categoryId: String? = null,
    val categoryName: String? = null
)

@Serializable
data class FoodPreferenceCategoryOptionDto(
    val id: String,
    val name: String,
    val system: Boolean
)
