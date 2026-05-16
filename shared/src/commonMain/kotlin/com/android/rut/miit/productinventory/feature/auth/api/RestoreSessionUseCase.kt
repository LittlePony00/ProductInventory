package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase

class RestoreSessionUseCase(
    private val tokenStorage: TokenStorage,
    private val getProfileUseCase: GetProfileUseCase
) {
    suspend operator fun invoke(): Boolean {
        val hasStoredTokens =
            tokenStorage.getAccessToken() != null && tokenStorage.getRefreshToken() != null
        if (!hasStoredTokens) return false

        return runCatching { getProfileUseCase() }
            .fold(
                onSuccess = { true },
                onFailure = {
                    tokenStorage.clearTokens()
                    false
                }
            )
    }
}
