package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions

class GetFoodPreferenceOptionsUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(householdId: String): FoodPreferenceOptions =
        repository.getFoodPreferenceOptions(householdId)
}
