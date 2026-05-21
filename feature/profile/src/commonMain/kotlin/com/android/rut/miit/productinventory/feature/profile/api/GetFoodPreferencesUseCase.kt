package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences

class GetFoodPreferencesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): FoodPreferences = repository.getFoodPreferences()
}
