package com.android.rut.miit.productinventory.feature.profile.api.models

data class UserProfile(
    val id: String,
    val email: String,
    val name: String
)

data class FoodPreferences(
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

data class FoodPreferenceOptions(
    val products: List<FoodPreferenceProductOption> = emptyList(),
    val categories: List<FoodPreferenceCategoryOption> = emptyList()
) {
    val hasStructuredOptions: Boolean = products.isNotEmpty() || categories.isNotEmpty()
}

data class FoodPreferenceProductOption(
    val id: String,
    val name: String,
    val categoryId: String? = null,
    val categoryName: String? = null
)

data class FoodPreferenceCategoryOption(
    val id: String,
    val name: String,
    val system: Boolean
)
