package com.android.rut.miit.productinventory.feature.profile.api

import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile

class GetProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): UserProfile = repository.getProfile()
}
