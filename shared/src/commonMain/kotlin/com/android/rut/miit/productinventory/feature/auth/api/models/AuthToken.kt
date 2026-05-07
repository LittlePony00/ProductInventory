package com.android.rut.miit.productinventory.feature.auth.api.models

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)
