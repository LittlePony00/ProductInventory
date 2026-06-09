package com.android.rut.miit.productinventory.feature.recommendations.api.models

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String? = null,
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val cookingTimeMinutes: Int? = null,
    val calories: Int,
    val caloriesKnown: Boolean = true,
    val source: String = "LOCAL_KNOWLEDGE_BASE",
    val sourceName: String? = null,
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
) {
    val localIdentity: String
        get() = listOf(
            id.orEmpty(),
            title,
            source,
            sourceName.orEmpty(),
            sourceUrl.orEmpty(),
            time,
            ingredients.joinToString(separator = "|") { "${it.name}:${it.amount}" },
            steps.joinToString(separator = "|")
        )
            .joinToString(separator = "\u001F")
}

@Serializable
data class RecipeIngredient(
    val name: String,
    val amount: String
)

enum class RecommendationMode {
    CURRENT_PRODUCTS,
    USE_SOON,
    AI_GENERATED_CUSTOM
}

enum class RecipeQuickFilter {
    UNDER_30_MIN,
    FEW_MISSING_INGREDIENTS,
    AI_GENERATED
}

data class RecipeIngredientOption(
    val id: String,
    val name: String,
    val categoryName: String?,
    val remainingAmount: Double,
    val unit: String,
    val expiring: Boolean
)

data class RecipeSearchRequest(
    val selectedProductIds: Set<String> = emptySet()
)

data class AiRecipeGenerationRequest(
    val maxCookingTimeMinutes: Int? = null,
    val servings: Int? = null,
    val extraNotes: String? = null
)

fun Recipe.localIdentity(): String =
    localIdentity
