package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferenceOptionsUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.UpdateFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceCategoryOption
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceProductOption
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
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

            viewModel.onEvent(ProfileEvent.OnCreate())
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
    fun `loads and updates food preferences`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository, FakeTokenStorage())

            viewModel.onEvent(ProfileEvent.OnCreate())
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnEditFoodPreferencesClick)
            viewModel.onEvent(ProfileEvent.OnPreferredProductsChanged("рис, томаты"))
            viewModel.onEvent(ProfileEvent.OnAvoidedProductsChanged("майонез"))
            viewModel.onEvent(ProfileEvent.OnAllergiesChanged("milk, nuts"))
            viewModel.onEvent(ProfileEvent.OnDietaryRestrictionsChanged("веганство"))
            viewModel.onEvent(ProfileEvent.OnMaxCookingTimeChanged("30"))
            viewModel.onEvent(ProfileEvent.OnPreferredDifficultyChanged("легко"))
            viewModel.onEvent(ProfileEvent.OnServingsChanged("2"))
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnSaveFoodPreferencesClick)
            advanceUntilIdle()

            val state = assertIs<ProfileState.Content>(viewModel.viewState.value)
            assertEquals(setOf("рис", "томаты"), state.foodPreferences.preferredProducts)
            assertEquals(setOf("майонез"), state.foodPreferences.avoidedProducts)
            assertEquals(setOf("milk", "nuts"), state.foodPreferences.allergies)
            assertEquals(setOf("VEGAN"), state.foodPreferences.dietaryRestrictions)
            assertEquals(30, state.foodPreferences.maxCookingTimeMinutes)
            assertEquals("EASY", state.foodPreferences.preferredDifficulty)
            assertEquals(2, state.foodPreferences.servings)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads household options and updates structured product and category preferences`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProfileRepository()
            val viewModel = viewModel(repository, FakeTokenStorage())

            viewModel.onEvent(ProfileEvent.OnCreate("household-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnEditFoodPreferencesClick)
            viewModel.onEvent(ProfileEvent.OnPreferredProductToggled("rice-id"))
            viewModel.onEvent(ProfileEvent.OnAvoidedProductToggled("milk-id"))
            viewModel.onEvent(ProfileEvent.OnPreferredCategoryToggled("cereal-id"))
            viewModel.onEvent(ProfileEvent.OnAvoidedCategoryToggled("dairy-id"))
            advanceUntilIdle()
            viewModel.onEvent(ProfileEvent.OnSaveFoodPreferencesClick)
            advanceUntilIdle()

            val state = assertIs<ProfileState.Content>(viewModel.viewState.value)
            assertEquals(setOf("rice-id"), state.foodPreferences.preferredProductIds)
            assertEquals(setOf("milk-id"), state.foodPreferences.avoidedProductIds)
            assertEquals(setOf("cereal-id"), state.foodPreferences.preferredCategoryIds)
            assertEquals(setOf("dairy-id"), state.foodPreferences.avoidedCategoryIds)
            assertEquals("household-id", repository.optionsHouseholdId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads profile and text preferences when household options fail`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeProfileRepository(optionsError = IllegalStateException("offline"))
            val viewModel = viewModel(repository, FakeTokenStorage())

            viewModel.onEvent(ProfileEvent.OnCreate("household-id"))
            advanceUntilIdle()

            val state = assertIs<ProfileState.Content>(viewModel.viewState.value)
            assertEquals("User", state.profile.name)
            assertEquals(false, state.foodPreferenceOptions.hasStructuredOptions)
            assertEquals("household-id", repository.optionsHouseholdId)
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
            getFoodPreferencesUseCase = GetFoodPreferencesUseCase(repository),
            getFoodPreferenceOptionsUseCase = GetFoodPreferenceOptionsUseCase(repository),
            updateProfileUseCase = UpdateProfileUseCase(repository),
            updateFoodPreferencesUseCase = UpdateFoodPreferencesUseCase(repository),
            tokenStorage = tokenStorage
        )

    private class FakeProfileRepository(
        private val optionsError: Throwable? = null
    ) : ProfileRepository {
        var profile = UserProfile(id = "user-id", email = "user@example.com", name = "User")
        var foodPreferences = FoodPreferences()
        var optionsHouseholdId: String? = null

        override suspend fun getProfile(): UserProfile = profile
        override suspend fun updateProfile(name: String): UserProfile {
            profile = profile.copy(name = name)
            return profile
        }

        override suspend fun getFoodPreferences(): FoodPreferences = foodPreferences

        override suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferenceOptions {
            optionsHouseholdId = householdId
            optionsError?.let { throw it }
            return FoodPreferenceOptions(
                products = listOf(
                    FoodPreferenceProductOption("rice-id", "Rice"),
                    FoodPreferenceProductOption("milk-id", "Milk")
                ),
                categories = listOf(
                    FoodPreferenceCategoryOption("cereal-id", "Крупы и злаки", system = true),
                    FoodPreferenceCategoryOption("dairy-id", "Молочные продукты", system = true)
                )
            )
        }

        override suspend fun updateFoodPreferences(preferences: FoodPreferences): FoodPreferences {
            foodPreferences = preferences
            return foodPreferences
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
