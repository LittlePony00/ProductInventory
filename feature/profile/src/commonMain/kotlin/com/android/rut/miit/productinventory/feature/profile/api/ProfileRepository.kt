package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun updateProfile(name: String): UserProfile
    suspend fun getFoodPreferences(): FoodPreferences
    suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferenceOptions
    suspend fun updateFoodPreferences(preferences: FoodPreferences): FoodPreferences
}
