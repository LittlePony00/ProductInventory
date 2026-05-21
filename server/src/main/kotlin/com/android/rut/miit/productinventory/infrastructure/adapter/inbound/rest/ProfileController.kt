package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.UpdateProfileRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateFoodPreferencesRequest
import com.android.rut.miit.productinventory.application.dto.response.FoodPreferencesResponse
import com.android.rut.miit.productinventory.application.dto.response.FoodPreferencesOptionsResponse
import com.android.rut.miit.productinventory.application.dto.response.UserResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IUserFoodPreferencesService
import com.android.rut.miit.productinventory.domain.port.inbound.IUserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController(
    private val userService: IUserService,
    private val foodPreferencesService: IUserFoodPreferencesService
) {

    @GetMapping
    fun getProfile(): UserResponse {
        return userService.getProfile(currentUserId()).toResponse()
    }

    @PutMapping
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): UserResponse {
        return userService.updateProfile(currentUserId(), request.name).toResponse()
    }

    @GetMapping("/food-preferences")
    fun getFoodPreferences(): FoodPreferencesResponse {
        return foodPreferencesService.getPreferences(currentUserId()).toResponse()
    }

    @GetMapping("/food-preferences/options")
    fun getFoodPreferenceOptions(
        @RequestParam householdId: java.util.UUID
    ): FoodPreferencesOptionsResponse {
        return foodPreferencesService.getPreferenceOptions(currentUserId(), householdId).toResponse()
    }

    @PutMapping("/food-preferences")
    fun updateFoodPreferences(
        @Valid @RequestBody request: UpdateFoodPreferencesRequest
    ): FoodPreferencesResponse {
        return foodPreferencesService.updatePreferences(
            userId = currentUserId(),
            preferredCuisines = request.preferredCuisines,
            preferredProducts = request.preferredProducts,
            dislikedIngredients = request.dislikedIngredients,
            avoidedProducts = request.avoidedProducts,
            allergies = request.allergies,
            dietaryRestrictions = request.dietaryRestrictions,
            preferredProductIds = request.preferredProductIds,
            avoidedProductIds = request.avoidedProductIds,
            preferredCategoryIds = request.preferredCategoryIds,
            avoidedCategoryIds = request.avoidedCategoryIds,
            maxCookingTimeMinutes = request.maxCookingTimeMinutes,
            preferredDifficulty = request.preferredDifficulty,
            servings = request.servings
        ).toResponse()
    }
}
