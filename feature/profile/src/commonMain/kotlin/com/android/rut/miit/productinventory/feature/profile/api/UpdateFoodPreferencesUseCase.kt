package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences

class UpdateFoodPreferencesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(preferences: FoodPreferences): FoodPreferences =
        repository.updateFoodPreferences(preferences)
}
