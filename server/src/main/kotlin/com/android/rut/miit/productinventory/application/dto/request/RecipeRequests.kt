package com.android.rut.miit.productinventory.application.dto.request

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

/**
 * Empty `selectedProductIds` means "random recipe with any products", not "all current household products".
 */
data class FindRecipeRequest(
    val selectedProductIds: Set<UUID> = emptySet()
)

data class GenerateAiRecipeRequest(
    @field:Positive(message = "Max cooking time must be positive")
    val maxCookingTimeMinutes: Int? = null,

    @field:Positive(message = "Servings must be positive")
    val servings: Int? = null,

    @field:Size(max = 500, message = "Extra notes must be at most 500 characters")
    val extraNotes: String? = null
)
