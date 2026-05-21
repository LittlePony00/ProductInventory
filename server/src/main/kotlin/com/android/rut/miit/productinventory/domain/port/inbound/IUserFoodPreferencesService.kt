package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.CookingDifficulty
import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import com.android.rut.miit.productinventory.domain.model.FoodPreferencesOptions
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import java.util.UUID

interface IUserFoodPreferencesService {
    fun getPreferences(userId: UUID): UserFoodPreferences
    fun getPreferenceOptions(userId: UUID, householdId: UUID): FoodPreferencesOptions

    fun updatePreferences(
        userId: UUID,
        preferredCuisines: Collection<String>,
        preferredProducts: Collection<String>,
        dislikedIngredients: Collection<String>,
        avoidedProducts: Collection<String>,
        allergies: Collection<String>,
        dietaryRestrictions: Collection<DietaryRestriction>,
        preferredProductIds: Collection<UUID>,
        avoidedProductIds: Collection<UUID>,
        preferredCategoryIds: Collection<UUID>,
        avoidedCategoryIds: Collection<UUID>,
        maxCookingTimeMinutes: Int?,
        preferredDifficulty: CookingDifficulty?,
        servings: Int?
    ): UserFoodPreferences
}
