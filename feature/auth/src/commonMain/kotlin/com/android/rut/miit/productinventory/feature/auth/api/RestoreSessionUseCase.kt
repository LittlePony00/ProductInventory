package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.core.storage.TokenStorage

class RestoreSessionUseCase(
    private val tokenStorage: TokenStorage
) {
    suspend operator fun invoke(): Boolean {
        return tokenStorage.getAccessToken() != null && tokenStorage.getRefreshToken() != null
    }
}
