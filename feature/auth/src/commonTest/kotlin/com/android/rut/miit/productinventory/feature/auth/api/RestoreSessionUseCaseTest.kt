package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.core.storage.TokenStorage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RestoreSessionUseCaseTest {

    @Test
    fun `returns true when stored tokens exist without requiring network validation`() = runTest {
        val tokenStorage = FakeTokenStorage().apply {
            saveTokens("access", "refresh")
        }
        val useCase = RestoreSessionUseCase(tokenStorage)

        assertTrue(useCase())
    }

    @Test
    fun `returns false without stored tokens`() = runTest {
        val tokenStorage = FakeTokenStorage()
        val useCase = RestoreSessionUseCase(tokenStorage)

        assertFalse(useCase())
    }

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = null
        private var refreshToken: String? = null
        private var userId: String? = null

        override fun getAccessToken(): String? = accessToken
        override fun getRefreshToken(): String? = refreshToken
        override fun saveTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }

        override fun clearTokens() {
            accessToken = null
            refreshToken = null
            userId = null
        }

        override fun getUserId(): String? = userId
        override fun saveUserId(userId: String) {
            this.userId = userId
        }
    }
}
