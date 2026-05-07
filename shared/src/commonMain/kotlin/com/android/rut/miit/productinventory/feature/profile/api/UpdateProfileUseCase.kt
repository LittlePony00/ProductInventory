package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile

class UpdateProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String): UserProfile = repository.updateProfile(name)
}
