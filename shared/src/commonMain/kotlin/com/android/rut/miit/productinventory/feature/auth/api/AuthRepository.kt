package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken

interface AuthRepository {
    suspend fun register(email: String, password: String, name: String): AuthToken
    suspend fun login(email: String, password: String): AuthToken
    suspend fun logout()
    fun isLoggedIn(): Boolean
}
