package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeRecommendationScorerTest {

    @Test
    fun `boosts expiring products strongly`() {
        val scorer = RecipeRecommendationScorer()
        val fresh = candidate(product("Rice", LocalDate.now().plusDays(10)))
        val expiring = candidate(product("Rice", LocalDate.now().plusDays(1)))
        val context = RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = emptyList(),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID())
        )

        val freshScore = scorer.score(fresh, context, safetyResult()).score
        val expiringScore = scorer.score(expiring, context, safetyResult()).score

        assertTrue(expiringScore > freshScore)
    }

    @Test
    fun `penalizes recipes over max cooking time`() {
        val scorer = RecipeRecommendationScorer()
        val context = RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = emptyList(),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences(
                userId = UUID.randomUUID(),
                maxCookingTimeMinutes = 15
            )
        )

        val score = scorer.score(candidate(product("Rice", LocalDate.now().plusDays(10)), time = "40 minutes"), context, safetyResult())

        assertTrue(score.score < scorer.score(candidate(product("Rice", LocalDate.now().plusDays(10)), time = "10 minutes"), context, safetyResult()).score)
    }

    @Test
    fun `boosts free-form preferred product matches`() {
        val scorer = RecipeRecommendationScorer()
        val matchedCandidate = candidate(product("Tomatoes", LocalDate.now().plusDays(10)))
        val baseContext = RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = emptyList(),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID())
        )
        val preferredContext = baseContext.copy(
            preferences = UserFoodPreferences(
                userId = baseContext.userId,
                preferredProducts = setOf("томат")
            )
        )

        val baseScore = scorer.score(matchedCandidate, baseContext, safetyResult()).score
        val preferredScore = scorer.score(matchedCandidate, preferredContext, safetyResult()).score

        assertTrue(preferredScore > baseScore)
    }

    @Test
    fun `returns localized missing ingredient display names`() {
        val scorer = RecipeRecommendationScorer()
        val rice = product("Рис", LocalDate.now().plusDays(10))
        val document = RecipeDocument(
            id = "rice-with-vegetables",
            title = "Рис с овощами",
            ingredients = listOf(
                RecipeIngredient("рис", "1 стакан"),
                RecipeIngredient("овощи", "200 г")
            ),
            steps = listOf("Приготовить"),
            time = "20 минут",
            calories = 220,
            requiredIngredients = setOf("rice", "vegetables"),
            categories = setOf(ProductCategory.CEREALS, ProductCategory.VEGETABLES_FRUITS),
            rules = emptyList()
        )
        val candidate = RecipeCandidate(
            document = document,
            match = RecipeDocumentMatch(
                document = document,
                score = 1.0,
                matchedProducts = listOf(rice),
                appliedRules = emptyList()
            )
        )
        val context = RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = listOf(rice),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID())
        )

        val score = scorer.score(candidate, context, safetyResult())

        assertEquals(listOf("овощи"), score.missingIngredients)
    }

    private fun safetyResult(): SafetyResult =
        SafetyResult(
            safe = true,
            warnings = emptyList(),
            dislikedIngredientMatches = emptySet(),
            dietaryRestrictionViolations = emptySet(),
            allergyMatches = emptySet()
        )

    private fun candidate(product: Product, time: String = "10 minutes"): RecipeCandidate {
        val document = RecipeDocument(
            id = "rice",
            title = "Rice",
            ingredients = listOf(RecipeIngredient("rice", "1 cup")),
            steps = listOf("Cook"),
            time = time,
            calories = 100,
            requiredIngredients = setOf("rice"),
            categories = setOf(ProductCategory.CEREALS),
            rules = emptyList()
        )
        return RecipeCandidate(
            document = document,
            match = RecipeDocumentMatch(
                document = document,
                score = 1.0,
                matchedProducts = listOf(product),
                appliedRules = emptyList()
            )
        )
    }

    private fun product(name: String, expirationDate: LocalDate): Product =
        Product(
            name = name,
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            expirationDate = ExpirationDate(expirationDate),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
}
