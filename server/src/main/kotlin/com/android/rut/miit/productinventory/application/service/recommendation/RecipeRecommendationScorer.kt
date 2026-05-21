package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.model.preferenceCategoryId
import org.springframework.stereotype.Component

@Component
class RecipeRecommendationScorer {

    fun score(
        candidate: RecipeCandidate,
        context: RecommendationContext,
        safetyResult: SafetyResult
    ): RecommendationScore {
        val requiredIngredients = candidate.document.requiredIngredients.map { it.normalizeIngredient() }
        val matchedProducts = candidate.match.matchedProducts
        val matchedNames = matchedProducts.flatMap { ingredientTerms(it.name) }.toSet()
        val matchedRequired = requiredIngredients.filter { required ->
            matchedNames.matchesAnyPreferenceTerm(ingredientTerms(required))
        }
        val missingIngredients = requiredIngredients.filterNot { required ->
            matchedRequired.any { it == required }
        }.map { required -> candidate.document.displayNameForRequiredIngredient(required) }
        val recipe = candidate.document.toRecipe()
        val cookingTimeMinutes = recipe.cookingTimeMinutes()
        val preferenceBonus = context.preferences.preferredCuisines.count { cuisine ->
            val normalized = cuisine.normalizeIngredient()
            candidate.document.title.normalizeIngredient().contains(normalized) ||
                candidate.document.rules.any { it.normalizeIngredient().contains(normalized) }
        } * 5.0
        val cookingTimeBonus = when {
            context.preferences.maxCookingTimeMinutes == null || cookingTimeMinutes == null -> 0.0
            cookingTimeMinutes <= context.preferences.maxCookingTimeMinutes -> 10.0
            else -> -15.0
        }
        val expirationBonus = matchedProducts.sumOf { it.expirationScore(context.mode) }
        val coverageScore = matchedRequired.size.toDouble() / requiredIngredients.size.coerceAtLeast(1) * 100.0
        val missingPenalty = missingIngredients.size * 12.0
        val dislikedPenalty = safetyResult.dislikedIngredientMatches.size * 20.0
        val dietBonus = context.preferences.dietaryRestrictions.size * 2.0
        val preferredProductBonus = matchedProducts.count { it.id in context.preferences.preferredProductIds } * 18.0
        val preferredProductTerms = context.preferences.preferredProducts.flatMap(::ingredientTerms).toSet()
        val preferredFreeformProductBonus = matchedProducts.count { product ->
            preferredProductTerms.isNotEmpty() && product.preferenceTerms().matchesAnyPreferenceTerm(preferredProductTerms)
        } * 12.0
        val preferredCategoryIds = matchedProducts.map { it.preferenceCategoryId() }.toSet() +
            candidate.document.categories.map(com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog::idFor)
        val preferredCategoryBonus = (preferredCategoryIds intersect context.preferences.preferredCategoryIds).size * 12.0

        return RecommendationScore(
            score = candidate.match.score + coverageScore + expirationBonus + preferenceBonus + cookingTimeBonus + dietBonus +
                preferredProductBonus + preferredFreeformProductBonus + preferredCategoryBonus -
                missingPenalty - dislikedPenalty,
            cookingTimeMinutes = cookingTimeMinutes,
            usedHouseholdProducts = matchedProducts.map(Product::name).distinct(),
            usedExpiringProducts = matchedProducts
                .filter { it.expirationDate?.status in setOf(ExpirationStatus.EXPIRED, ExpirationStatus.EXPIRING_SOON) }
                .map(Product::name)
                .distinct(),
            missingIngredients = missingIngredients
        )
    }
}

data class RecommendationScore(
    val score: Double,
    val cookingTimeMinutes: Int?,
    val usedHouseholdProducts: List<String>,
    val usedExpiringProducts: List<String>,
    val missingIngredients: List<String>
)

fun Recipe.cookingTimeMinutes(): Int? =
    numberPattern.find(time)?.value?.toIntOrNull()

private fun Product.expirationScore(mode: RecommendationMode): Double =
    when (expirationDate?.status) {
        ExpirationStatus.EXPIRED -> if (mode == RecommendationMode.USE_SOON) 55.0 else 35.0
        ExpirationStatus.EXPIRING_SOON -> if (mode == RecommendationMode.USE_SOON) 65.0 else 45.0
        ExpirationStatus.FRESH -> 4.0
        ExpirationStatus.UNKNOWN, null -> 0.0
    }

private val numberPattern = Regex("""\d+""")

private fun com.android.rut.miit.productinventory.domain.model.RecipeDocument.displayNameForRequiredIngredient(required: String): String =
    ingredients
        .firstOrNull { ingredient -> ingredientTerms(ingredient.name).matchesAnyPreferenceTerm(ingredientTerms(required)) }
        ?.name
        ?: required
