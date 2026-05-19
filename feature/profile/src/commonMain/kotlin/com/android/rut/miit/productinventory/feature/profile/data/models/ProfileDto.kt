package com.android.rut.miit.productinventory.feature.profile.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponseDto(
    val id: String,
    val email: String,
    val name: String
)

@Serializable
data class UpdateProfileRequestDto(
    val name: String
)
