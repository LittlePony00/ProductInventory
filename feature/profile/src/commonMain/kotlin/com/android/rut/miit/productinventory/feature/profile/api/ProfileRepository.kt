package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile
    suspend fun updateProfile(name: String): UserProfile
}
