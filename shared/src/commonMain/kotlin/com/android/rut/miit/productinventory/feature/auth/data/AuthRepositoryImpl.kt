package com.android.rut.miit.productinventory.feature.auth.data

import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.auth.api.AuthRepository
import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken
import com.android.rut.miit.productinventory.feature.auth.data.mappers.toDomain

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val deviceTokenRegistrar: DeviceTokenRegistrar
) : AuthRepository {

    override suspend fun register(email: String, password: String, name: String): AuthToken {
        val response = remoteDataSource.register(email, password, name)
        val token = response.toDomain()
        tokenStorage.saveTokens(token.accessToken, token.refreshToken)
        tokenStorage.saveUserId(token.userId)
        runCatching { deviceTokenRegistrar.registerCurrentToken() }
        return token
    }

    override suspend fun login(email: String, password: String): AuthToken {
        val response = remoteDataSource.login(email, password)
        val token = response.toDomain()
        tokenStorage.saveTokens(token.accessToken, token.refreshToken)
        tokenStorage.saveUserId(token.userId)
        runCatching { deviceTokenRegistrar.registerCurrentToken() }
        return token
    }

    override suspend fun logout() {
        tokenStorage.clearTokens()
    }

    override fun isLoggedIn(): Boolean {
        return tokenStorage.getAccessToken() != null
    }
}
