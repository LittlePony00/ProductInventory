package com.android.rut.miit.productinventory.application.dto.response

import java.util.UUID

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)
