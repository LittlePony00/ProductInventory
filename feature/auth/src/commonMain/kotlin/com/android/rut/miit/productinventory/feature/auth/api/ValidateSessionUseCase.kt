package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.core.network.ApiException
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase

class ValidateSessionUseCase(
    private val tokenStorage: TokenStorage,
    private val getProfileUseCase: GetProfileUseCase
) {
    suspend operator fun invoke(): Boolean {
        val hasStoredTokens = tokenStorage.getAccessToken() != null && tokenStorage.getRefreshToken() != null
        if (!hasStoredTokens) return false

        return runCatching { getProfileUseCase() }
            .fold(
                onSuccess = { true },
                onFailure = { error ->
                    if (error.isAuthenticationRejected()) {
                        tokenStorage.clearTokens()
                        false
                    } else {
                        true
                    }
                }
            )
    }

    private fun Throwable.isAuthenticationRejected(): Boolean =
        this is ApiException && statusCode in AUTH_REJECTION_STATUS_CODES

    private companion object {
        val AUTH_REJECTION_STATUS_CODES = setOf(401, 403)
    }
}
