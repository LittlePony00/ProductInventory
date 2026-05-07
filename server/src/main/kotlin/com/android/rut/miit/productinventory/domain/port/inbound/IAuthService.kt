package com.android.rut.miit.productinventory.domain.port.inbound

import java.util.UUID

interface IAuthService {
    fun register(email: String, password: String, name: String): AuthResult
    fun login(email: String, password: String): AuthResult
    fun refreshToken(refreshToken: String): AuthResult
}

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: UUID
)
