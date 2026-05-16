package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ProfileViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads and updates profile`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository, FakeTokenStorage())

            viewModel.onEvent(ProfileEvent.OnCreate)
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnEditClick)
            viewModel.onEvent(ProfileEvent.OnNameChanged("New name"))
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnSaveClick)
            advanceUntilIdle()

            val state = assertIs<ProfileState.Content>(viewModel.viewState.value)
            assertEquals("New name", state.profile.name)
            assertEquals("New name", repository.profile.name)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `logout clears tokens and navigates to login`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val tokenStorage = FakeTokenStorage().apply {
                saveTokens("access", "refresh")
                saveUserId("user-id")
            }
            val viewModel = viewModel(FakeProfileRepository(), tokenStorage)
            val nextAction = async { viewModel.viewAction.first() }

            viewModel.onEvent(ProfileEvent.OnLogoutClick)
            advanceUntilIdle()

            assertNull(tokenStorage.getAccessToken())
            assertNull(tokenStorage.getRefreshToken())
            assertIs<ProfileAction.NavigateToLogin>(nextAction.await())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(
        repository: ProfileRepository,
        tokenStorage: TokenStorage
    ): ProfileViewModel =
        ProfileViewModel(
            getProfileUseCase = GetProfileUseCase(repository),
            updateProfileUseCase = UpdateProfileUseCase(repository),
            tokenStorage = tokenStorage
        )

    private class FakeProfileRepository : ProfileRepository {
        var profile = UserProfile(id = "user-id", email = "user@example.com", name = "User")

        override suspend fun getProfile(): UserProfile = profile
        override suspend fun updateProfile(name: String): UserProfile {
            profile = profile.copy(name = name)
            return profile
        }
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
