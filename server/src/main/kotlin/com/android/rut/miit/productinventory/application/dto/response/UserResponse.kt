package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.CookingDifficulty
import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String
)

data class FoodPreferencesResponse(
    val preferredCuisines: Set<String>,
    val preferredProducts: Set<String>,
    val dislikedIngredients: Set<String>,
    val avoidedProducts: Set<String>,
    val allergies: Set<String>,
    val dietaryRestrictions: Set<DietaryRestriction>,
    val preferredProductIds: Set<UUID>,
    val avoidedProductIds: Set<UUID>,
    val preferredCategoryIds: Set<UUID>,
    val avoidedCategoryIds: Set<UUID>,
    val maxCookingTimeMinutes: Int?,
    val preferredDifficulty: CookingDifficulty?,
    val servings: Int?
)

data class FoodPreferencesOptionsResponse(
    val products: List<FoodPreferenceProductOptionResponse>,
    val categories: List<FoodPreferenceCategoryOptionResponse>
)

data class FoodPreferenceProductOptionResponse(
    val id: UUID,
    val name: String,
    val categoryId: UUID?,
    val categoryName: String?
)

data class FoodPreferenceCategoryOptionResponse(
    val id: UUID,
    val name: String,
    val system: Boolean
)
