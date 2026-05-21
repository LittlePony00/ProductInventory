package com.android.rut.miit.productinventory.feature.profile.data

import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import com.android.rut.miit.productinventory.feature.profile.data.mappers.toRequestDto
import com.android.rut.miit.productinventory.feature.profile.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.profile.data.models.UpdateProfileRequestDto

class ProfileRepositoryImpl(
    private val remoteDataSource: ProfileRemoteDataSource
) : ProfileRepository {

    override suspend fun getProfile(): UserProfile {
        return remoteDataSource.getProfile().toDomain()
    }

    override suspend fun updateProfile(name: String): UserProfile {
        return remoteDataSource.updateProfile(UpdateProfileRequestDto(name)).toDomain()
    }

    override suspend fun getFoodPreferences(): FoodPreferences {
        return remoteDataSource.getFoodPreferences().toDomain()
    }

    override suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferenceOptions {
        return remoteDataSource.getFoodPreferenceOptions(householdId).toDomain()
    }

    override suspend fun updateFoodPreferences(preferences: FoodPreferences): FoodPreferences {
        return remoteDataSource.updateFoodPreferences(preferences.toRequestDto()).toDomain()
    }
}
