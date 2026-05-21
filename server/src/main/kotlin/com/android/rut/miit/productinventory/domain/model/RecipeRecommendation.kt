package com.android.rut.miit.productinventory.domain.model

enum class RecommendationMode {
    CURRENT_PRODUCTS,
    USE_SOON,
    AI_GENERATED_CUSTOM
}

enum class RecipeSource {
    LOCAL_KNOWLEDGE_BASE,
    EXTERNAL_API,
    AI_ASSISTED,
    AI_GENERATED
}

data class RecipeRecommendation(
    val id: String? = null,
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val cookingTimeMinutes: Int? = null,
    val calories: Int,
    val caloriesKnown: Boolean = true,
    val source: RecipeSource = RecipeSource.LOCAL_KNOWLEDGE_BASE,
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

data class RecipeIngredientOption(
    val id: java.util.UUID,
    val name: String,
    val categoryName: String?,
    val remainingAmount: Double,
    val unit: QuantityUnit,
    val expiring: Boolean
)

/**
 * Empty `selectedProductIds` means an unconstrained random recipe search with any products.
 * It must not be interpreted as "use all current household products".
 */
data class RecipeSearchRequest(
    val selectedProductIds: Set<java.util.UUID> = emptySet()
)

data class RecipeDiscoveryResult(
    val recipe: Recipe,
    val source: RecipeSource,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val reasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val aiAssisted: Boolean = false,
    val requiresLocalization: Boolean = source == RecipeSource.EXTERNAL_API
)

data class AiRecipeGenerationRequest(
    val maxCookingTimeMinutes: Int? = null,
    val servings: Int? = null,
    val extraNotes: String? = null
)

data class AiRecipeGenerationContext(
    val products: List<Product>,
    val preferences: UserFoodPreferences,
    val request: AiRecipeGenerationRequest,
    val allProducts: List<Product> = products
)
