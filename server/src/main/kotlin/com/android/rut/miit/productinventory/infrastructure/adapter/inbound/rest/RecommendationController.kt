package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.FindRecipeRequest
import com.android.rut.miit.productinventory.application.dto.request.GenerateAiRecipeRequest
import com.android.rut.miit.productinventory.application.dto.response.RecipeIngredientOptionResponse
import com.android.rut.miit.productinventory.application.dto.response.RecipeResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.port.inbound.IRecommendationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/recipes")
class RecommendationController(
    private val recommendationService: IRecommendationService
) {

    @GetMapping
    fun getRecipes(
        @PathVariable householdId: UUID,
        @RequestParam(defaultValue = "CURRENT_PRODUCTS") mode: RecommendationMode = RecommendationMode.CURRENT_PRODUCTS
    ): List<RecipeResponse> {
        return recommendationService.getRecipes(currentUserId(), householdId, mode)
            .map { it.toResponse() }
    }

    @GetMapping("/suggestions")
    fun getRecipeSuggestions(@PathVariable householdId: UUID): List<RecipeResponse> {
        return recommendationService.getRecipes(currentUserId(), householdId, RecommendationMode.CURRENT_PRODUCTS)
            .map { it.toResponse() }
    }

    @GetMapping("/ingredients")
    fun getIngredientOptions(@PathVariable householdId: UUID): List<RecipeIngredientOptionResponse> =
        recommendationService.getIngredientOptions(currentUserId(), householdId)
            .map { it.toResponse() }

    @PostMapping("/search")
    fun findRecipes(
        @PathVariable householdId: UUID,
        @Valid @RequestBody request: FindRecipeRequest
    ): List<RecipeResponse> =
        recommendationService.findRecipes(
            userId = currentUserId(),
            householdId = householdId,
            request = RecipeSearchRequest(selectedProductIds = request.selectedProductIds)
        ).map { it.toResponse() }

    @PostMapping("/ai-generated")
    fun generateAiRecipe(
        @PathVariable householdId: UUID,
        @Valid @RequestBody(required = false) request: GenerateAiRecipeRequest?
    ): RecipeResponse =
        recommendationService.generateAiRecipe(
            userId = currentUserId(),
            householdId = householdId,
            request = AiRecipeGenerationRequest(
                maxCookingTimeMinutes = request?.maxCookingTimeMinutes,
                servings = request?.servings,
                extraNotes = request?.extraNotes?.trim()?.takeIf(String::isNotEmpty)
            )
        ).toResponse()
}
