package com.android.rut.miit.productinventory.feature.profile.data.mappers

import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import com.android.rut.miit.productinventory.feature.profile.data.models.UserProfileResponseDto

fun UserProfileResponseDto.toDomain() = UserProfile(
    id = id,
    email = email,
    name = name
)
