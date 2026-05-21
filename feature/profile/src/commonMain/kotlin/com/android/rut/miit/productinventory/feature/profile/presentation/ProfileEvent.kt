package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.common.UiEvent

sealed class ProfileEvent : UiEvent {
    data class OnCreate(val householdId: String? = null) : ProfileEvent()
    data object OnRetry : ProfileEvent()
    data object OnEditClick : ProfileEvent()
    data class OnNameChanged(val name: String) : ProfileEvent()
    data object OnSaveClick : ProfileEvent()
    data object OnCancelEdit : ProfileEvent()
    data object OnEditFoodPreferencesClick : ProfileEvent()
    data class OnPreferredCuisinesChanged(val value: String) : ProfileEvent()
    data class OnPreferredProductsChanged(val value: String) : ProfileEvent()
    data class OnDislikedIngredientsChanged(val value: String) : ProfileEvent()
    data class OnAvoidedProductsChanged(val value: String) : ProfileEvent()
    data class OnAllergiesChanged(val value: String) : ProfileEvent()
    data class OnDietaryRestrictionsChanged(val value: String) : ProfileEvent()
    data class OnPreferredProductToggled(val id: String) : ProfileEvent()
    data class OnAvoidedProductToggled(val id: String) : ProfileEvent()
    data class OnPreferredCategoryToggled(val id: String) : ProfileEvent()
    data class OnAvoidedCategoryToggled(val id: String) : ProfileEvent()
    data class OnMaxCookingTimeChanged(val value: String) : ProfileEvent()
    data class OnPreferredDifficultyChanged(val value: String) : ProfileEvent()
    data class OnServingsChanged(val value: String) : ProfileEvent()
    data object OnSaveFoodPreferencesClick : ProfileEvent()
    data object OnCancelFoodPreferencesEdit : ProfileEvent()
    data object OnLogoutClick : ProfileEvent()
    data object OnBackClick : ProfileEvent()
}
