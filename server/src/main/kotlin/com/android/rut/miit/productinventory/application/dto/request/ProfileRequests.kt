package com.android.rut.miit.productinventory.application.dto.request

import com.android.rut.miit.productinventory.domain.model.CookingDifficulty
import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

data class UpdateProfileRequest(
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    val name: String? = null
)

data class UpdateFoodPreferencesRequest(
    @field:Size(max = 30, message = "Preferred cuisines must contain at most 30 items")
    val preferredCuisines: Set<String> = emptySet(),

    @field:Size(max = 30, message = "Preferred products must contain at most 30 items")
    val preferredProducts: Set<String> = emptySet(),

    @field:Size(max = 30, message = "Disliked ingredients must contain at most 30 items")
    val dislikedIngredients: Set<String> = emptySet(),

    @field:Size(max = 30, message = "Avoided products must contain at most 30 items")
    val avoidedProducts: Set<String> = emptySet(),

    @field:Size(max = 30, message = "Allergies must contain at most 30 items")
    val allergies: Set<String> = emptySet(),

    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),

    @field:Size(max = 100, message = "Preferred products must contain at most 100 items")
    val preferredProductIds: Set<UUID> = emptySet(),

    @field:Size(max = 100, message = "Avoided products must contain at most 100 items")
    val avoidedProductIds: Set<UUID> = emptySet(),

    @field:Size(max = 100, message = "Preferred categories must contain at most 100 items")
    val preferredCategoryIds: Set<UUID> = emptySet(),

    @field:Size(max = 100, message = "Avoided categories must contain at most 100 items")
    val avoidedCategoryIds: Set<UUID> = emptySet(),

    @field:Positive(message = "Max cooking time must be positive")
    val maxCookingTimeMinutes: Int? = null,

    val preferredDifficulty: CookingDifficulty? = null,

    @field:Positive(message = "Servings must be positive")
    val servings: Int? = null
)
