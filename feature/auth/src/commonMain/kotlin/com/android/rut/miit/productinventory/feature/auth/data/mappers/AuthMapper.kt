package com.android.rut.miit.productinventory.feature.auth.data.mappers

import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken
import com.android.rut.miit.productinventory.feature.auth.data.models.AuthResponseDto

fun AuthResponseDto.toDomain() = AuthToken(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId
)
