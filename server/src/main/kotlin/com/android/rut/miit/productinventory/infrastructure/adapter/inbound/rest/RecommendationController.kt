package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.response.RecipeResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IRecommendationService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/recipes")
class RecommendationController(
    private val recommendationService: IRecommendationService
) {

    @GetMapping("", "/suggestions")
    fun getRecipes(@PathVariable householdId: UUID): List<RecipeResponse> {
        return recommendationService.getRecipes(currentUserId(), householdId)
            .map { it.toResponse() }
    }
}
