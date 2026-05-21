package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.core.network.ApiException
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ValidateSessionUseCaseTest {

    @Test
    fun `keeps session when profile endpoint succeeds`() = runTest {
        val tokenStorage = FakeTokenStorage().apply { saveTokens("access", "refresh") }
        val useCase = useCase(tokenStorage, FakeProfileRepository())

        assertTrue(useCase())
    }

    @Test
    fun `keeps local session when validation fails due to network`() = runTest {
        val tokenStorage = FakeTokenStorage().apply {
            saveTokens("access", "refresh")
            saveUserId("user-id")
        }
        val useCase = useCase(tokenStorage, FakeProfileRepository(error = IllegalStateException("offline")))

        assertTrue(useCase())
        assertTrue(tokenStorage.getAccessToken() != null)
        assertTrue(tokenStorage.getRefreshToken() != null)
    }

    @Test
    fun `clears tokens only when backend rejects authentication`() = runTest {
        val tokenStorage = FakeTokenStorage().apply {
            saveTokens("expired-access", "expired-refresh")
            saveUserId("user-id")
        }
        val useCase = useCase(tokenStorage, FakeProfileRepository(error = ApiException(401, "Unauthorized")))

        assertFalse(useCase())
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(tokenStorage.getUserId())
    }

    private fun useCase(
        tokenStorage: TokenStorage,
        repository: ProfileRepository
    ): ValidateSessionUseCase =
        ValidateSessionUseCase(
            tokenStorage = tokenStorage,
            getProfileUseCase = GetProfileUseCase(repository)
        )

    private class FakeProfileRepository(
        private val error: Throwable? = null
    ) : ProfileRepository {
        override suspend fun getProfile(): UserProfile {
            error?.let { throw it }
            return UserProfile(id = "user-id", email = "user@example.com", name = "User")
        }

        override suspend fun updateProfile(name: String): UserProfile =
            UserProfile(id = "user-id", email = "user@example.com", name = name)

        override suspend fun getFoodPreferences(): FoodPreferences = FoodPreferences()
        override suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferenceOptions = FoodPreferenceOptions()
        override suspend fun updateFoodPreferences(preferences: FoodPreferences): FoodPreferences = preferences
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
