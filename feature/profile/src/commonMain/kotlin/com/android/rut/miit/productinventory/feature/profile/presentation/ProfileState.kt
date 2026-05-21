package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile

sealed class ProfileState : UiState {
    data object Loading : ProfileState()
    data class Content(
        val profile: UserProfile,
        val foodPreferences: FoodPreferences = FoodPreferences(),
        val foodPreferenceOptions: FoodPreferenceOptions = FoodPreferenceOptions(),
        val householdId: String? = null,
        val isEditing: Boolean = false,
        val editName: String = "",
        val isEditingFoodPreferences: Boolean = false,
        val editPreferredCuisines: String = "",
        val editPreferredProducts: String = "",
        val editDislikedIngredients: String = "",
        val editAvoidedProducts: String = "",
        val editAllergies: String = "",
        val editDietaryRestrictions: String = "",
        val editPreferredProductIds: Set<String> = emptySet(),
        val editAvoidedProductIds: Set<String> = emptySet(),
        val editPreferredCategoryIds: Set<String> = emptySet(),
        val editAvoidedCategoryIds: Set<String> = emptySet(),
        val editMaxCookingTimeMinutes: String = "",
        val editPreferredDifficulty: String = "",
        val editServings: String = ""
    ) : ProfileState()
    data class Error(val message: String?) : ProfileState()
}
