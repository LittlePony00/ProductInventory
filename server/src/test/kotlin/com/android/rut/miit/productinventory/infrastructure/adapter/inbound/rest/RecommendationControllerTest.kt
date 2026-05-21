package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.GenerateAiRecipeRequest
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeIngredientOption
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.port.inbound.IRecommendationService
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class RecommendationControllerTest {

    @AfterTest
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `recipes endpoint forwards selected mode`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = RecordingRecommendationService()
        authenticate(userId)

        RecommendationController(service).getRecipes(householdId, RecommendationMode.USE_SOON)

        assertEquals(listOf(RecommendationMode.USE_SOON), service.modes)
        assertEquals(listOf(userId), service.users)
    }

    @Test
    fun `suggestions endpoint remains current products compatibility alias`() {
        val service = RecordingRecommendationService()
        authenticate(UUID.randomUUID())

        RecommendationController(service).getRecipeSuggestions(UUID.randomUUID())

        assertEquals(listOf(RecommendationMode.CURRENT_PRODUCTS), service.modes)
    }

    @Test
    fun `ai generated endpoint forwards request overrides`() {
        val service = RecordingRecommendationService()
        authenticate(UUID.randomUUID())

        val response = RecommendationController(service).generateAiRecipe(
            householdId = UUID.randomUUID(),
            request = GenerateAiRecipeRequest(
                maxCookingTimeMinutes = 30,
                servings = 2,
                extraNotes = " quick dinner "
            )
        )

        assertEquals(true, response.aiGenerated)
        assertEquals(AiRecipeGenerationRequest(maxCookingTimeMinutes = 30, servings = 2, extraNotes = "quick dinner"), service.aiRequests.single())
    }

    @Test
    fun `ingredients endpoint returns current ingredient options`() {
        val service = RecordingRecommendationService()
        authenticate(UUID.randomUUID())

        val response = RecommendationController(service).getIngredientOptions(UUID.randomUUID())

        assertEquals("Рис", response.single().name)
    }

    @Test
    fun `search endpoint forwards selected product ids`() {
        val service = RecordingRecommendationService()
        authenticate(UUID.randomUUID())
        val productId = UUID.randomUUID()

        RecommendationController(service).findRecipes(
            householdId = UUID.randomUUID(),
            request = com.android.rut.miit.productinventory.application.dto.request.FindRecipeRequest(
                selectedProductIds = setOf(productId)
            )
        )

        assertEquals(RecipeSearchRequest(selectedProductIds = setOf(productId)), service.searchRequests.single())
    }

    @Test
    fun `search endpoint forwards empty selected product ids as random search`() {
        val service = RecordingRecommendationService()
        authenticate(UUID.randomUUID())

        RecommendationController(service).findRecipes(
            householdId = UUID.randomUUID(),
            request = com.android.rut.miit.productinventory.application.dto.request.FindRecipeRequest(
                selectedProductIds = emptySet()
            )
        )

        assertEquals(RecipeSearchRequest(selectedProductIds = emptySet()), service.searchRequests.single())
    }

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }

    private class RecordingRecommendationService : IRecommendationService {
        val users = mutableListOf<UUID>()
        val modes = mutableListOf<RecommendationMode>()
        val aiRequests = mutableListOf<AiRecipeGenerationRequest>()
        val searchRequests = mutableListOf<RecipeSearchRequest>()

        override fun getRecipes(
            userId: UUID,
            householdId: UUID,
            mode: RecommendationMode
        ): List<RecipeRecommendation> {
            users += userId
            modes += mode
            return listOf(recommendation())
        }

        override fun getIngredientOptions(userId: UUID, householdId: UUID): List<RecipeIngredientOption> =
            listOf(
                RecipeIngredientOption(
                    id = UUID.randomUUID(),
                    name = "Рис",
                    categoryName = "Крупы",
                    remainingAmount = 1.0,
                    unit = QuantityUnit.PIECES,
                    expiring = false
                )
            )

        override fun findRecipes(
            userId: UUID,
            householdId: UUID,
            request: RecipeSearchRequest
        ): List<RecipeRecommendation> {
            searchRequests += request
            return listOf(recommendation())
        }

        override fun generateAiRecipe(
            userId: UUID,
            householdId: UUID,
            request: AiRecipeGenerationRequest
        ): RecipeRecommendation {
            aiRequests += request
            return recommendation(aiGenerated = true)
        }
    }
}

private fun recommendation(aiGenerated: Boolean = false): RecipeRecommendation =
    RecipeRecommendation(
        title = "Recipe",
        ingredients = listOf(RecipeIngredient("rice", "1 cup")),
        steps = listOf("Cook"),
        time = "10 minutes",
        calories = 100,
        aiGenerated = aiGenerated
    )
