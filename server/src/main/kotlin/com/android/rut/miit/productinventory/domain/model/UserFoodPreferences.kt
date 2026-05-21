package com.android.rut.miit.productinventory.domain.model

import java.util.UUID

data class UserFoodPreferences(
    val userId: UUID,
    val preferredCuisines: Set<String> = emptySet(),
    val preferredProducts: Set<String> = emptySet(),
    val dislikedIngredients: Set<String> = emptySet(),
    val avoidedProducts: Set<String> = emptySet(),
    val allergies: Set<String> = emptySet(),
    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),
    val preferredProductIds: Set<UUID> = emptySet(),
    val avoidedProductIds: Set<UUID> = emptySet(),
    val preferredCategoryIds: Set<UUID> = emptySet(),
    val avoidedCategoryIds: Set<UUID> = emptySet(),
    val maxCookingTimeMinutes: Int? = null,
    val preferredDifficulty: CookingDifficulty? = null,
    val servings: Int? = null
) {
    companion object {
        fun empty(userId: UUID): UserFoodPreferences = UserFoodPreferences(userId = userId)
    }
}

enum class DietaryRestriction {
    VEGETARIAN,
    VEGAN,
    GLUTEN_FREE,
    DAIRY_FREE,
    NUT_FREE,
    HALAL,
    KOSHER
}

enum class CookingDifficulty {
    EASY,
    MEDIUM,
    HARD
}
