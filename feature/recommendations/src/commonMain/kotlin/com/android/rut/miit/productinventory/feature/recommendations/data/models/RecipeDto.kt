package com.android.rut.miit.productinventory.feature.recommendations.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RecipeResponseDto(
    val id: String? = null,
    val title: String,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<String>,
    val time: String,
    val cookingTimeMinutes: Int? = null,
    val calories: Int,
    val caloriesKnown: Boolean = true,
    val source: String = "LOCAL_KNOWLEDGE_BASE",
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val score: Double = 0.0,
    val usedHouseholdProducts: List<String> = emptyList(),
    val usedExpiringProducts: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val reasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val aiAssisted: Boolean = false,
    val aiGenerated: Boolean = false
)

@Serializable
data class RecipeIngredientDto(
    val name: String,
    val amount: String
)

@Serializable
data class RecipeIngredientOptionDto(
    val id: String,
    val name: String,
    val categoryName: String? = null,
    val remainingAmount: Double,
    val unit: String,
    val expiring: Boolean
)

@Serializable
data class FindRecipeRequestDto(
    val selectedProductIds: Set<String> = emptySet()
)

@Serializable
data class GenerateAiRecipeRequestDto(
    val maxCookingTimeMinutes: Int? = null,
    val servings: Int? = null,
    val extraNotes: String? = null
)
