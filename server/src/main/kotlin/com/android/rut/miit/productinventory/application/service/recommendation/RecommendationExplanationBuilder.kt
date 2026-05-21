package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import org.springframework.stereotype.Component

@Component
class RecommendationExplanationBuilder {

    fun build(
        candidate: RecipeCandidate,
        context: RecommendationContext,
        score: RecommendationScore,
        safetyResult: SafetyResult
    ): RecipeRecommendation =
        RecipeRecommendation(
            id = candidate.document.id,
            title = candidate.document.title,
            ingredients = candidate.document.ingredients,
            steps = candidate.document.steps,
            time = candidate.document.time,
            cookingTimeMinutes = score.cookingTimeMinutes,
            calories = candidate.document.calories,
            source = RecipeSource.LOCAL_KNOWLEDGE_BASE,
            score = score.score,
            usedHouseholdProducts = score.usedHouseholdProducts,
            usedExpiringProducts = score.usedExpiringProducts,
            missingIngredients = score.missingIngredients,
            reasons = reasons(context, score, candidate),
            warnings = safetyResult.warnings.filterNot { it.startsWith("Исключено") },
            aiGenerated = false
        )

    private fun reasons(
        context: RecommendationContext,
        score: RecommendationScore,
        candidate: RecipeCandidate
    ): List<String> =
        buildList {
            if (score.usedHouseholdProducts.isNotEmpty()) {
                add("Использует продукты из текущих запасов: ${score.usedHouseholdProducts.joinToString()}")
            }
            if (score.usedExpiringProducts.isNotEmpty()) {
                add("Помогает использовать продукты с близким сроком: ${score.usedExpiringProducts.joinToString()}")
            }
            if (context.mode == RecommendationMode.USE_SOON) {
                add("Режим «Использовать скоро» усиливает приоритет сроков годности")
            }
            context.preferences.maxCookingTimeMinutes?.let { maxMinutes ->
                score.cookingTimeMinutes?.takeIf { it <= maxMinutes }?.let {
                    add("Подходит по времени приготовления: $it мин")
                }
            }
            candidate.document.rules.take(2).forEach { add(it) }
        }.distinct()
}
