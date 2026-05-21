package com.android.rut.miit.productinventory.feature.profile.data.mappers

import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceCategoryOption
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceProductOption
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import com.android.rut.miit.productinventory.feature.profile.data.models.FoodPreferencesOptionsResponseDto
import com.android.rut.miit.productinventory.feature.profile.data.models.FoodPreferencesResponseDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UpdateFoodPreferencesRequestDto
import com.android.rut.miit.productinventory.feature.profile.data.models.UserProfileResponseDto

fun UserProfileResponseDto.toDomain() = UserProfile(
    id = id,
    email = email,
    name = name
)

fun FoodPreferencesResponseDto.toDomain() = FoodPreferences(
    preferredCuisines = preferredCuisines,
    preferredProducts = preferredProducts,
    dislikedIngredients = dislikedIngredients,
    avoidedProducts = avoidedProducts,
    allergies = allergies,
    dietaryRestrictions = dietaryRestrictions,
    preferredProductIds = preferredProductIds,
    avoidedProductIds = avoidedProductIds,
    preferredCategoryIds = preferredCategoryIds,
    avoidedCategoryIds = avoidedCategoryIds,
    maxCookingTimeMinutes = maxCookingTimeMinutes,
    preferredDifficulty = preferredDifficulty,
    servings = servings
)

fun FoodPreferences.toRequestDto() = UpdateFoodPreferencesRequestDto(
    preferredCuisines = preferredCuisines,
    preferredProducts = preferredProducts,
    dislikedIngredients = dislikedIngredients,
    avoidedProducts = avoidedProducts,
    allergies = allergies,
    dietaryRestrictions = dietaryRestrictions.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet(),
    preferredProductIds = preferredProductIds,
    avoidedProductIds = avoidedProductIds,
    preferredCategoryIds = preferredCategoryIds,
    avoidedCategoryIds = avoidedCategoryIds,
    maxCookingTimeMinutes = maxCookingTimeMinutes,
    preferredDifficulty = preferredDifficulty?.trim()?.uppercase()?.takeIf(String::isNotEmpty),
    servings = servings
)

fun FoodPreferencesOptionsResponseDto.toDomain() = FoodPreferenceOptions(
    products = products.map {
        FoodPreferenceProductOption(
            id = it.id,
            name = it.name,
            categoryId = it.categoryId,
            categoryName = it.categoryName
        )
    },
    categories = categories.map {
        FoodPreferenceCategoryOption(
            id = it.id,
            name = it.name,
            system = it.system
        )
    }
)
