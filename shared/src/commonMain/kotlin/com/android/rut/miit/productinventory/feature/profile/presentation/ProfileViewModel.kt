package com.android.rut.miit.productinventory.feature.profile.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val tokenStorage: TokenStorage
) : SharedViewModel<ProfileState, ProfileEvent, ProfileAction>(
    initialState = ProfileState.Loading
) {

    override suspend fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnCreate -> loadProfile()
            is ProfileEvent.OnRetry -> loadProfile()
            is ProfileEvent.OnEditClick -> onEditClick()
            is ProfileEvent.OnNameChanged -> onNameChanged(event.name)
            is ProfileEvent.OnSaveClick -> onSave()
            is ProfileEvent.OnCancelEdit -> onCancelEdit()
            is ProfileEvent.OnLogoutClick -> onLogout()
            is ProfileEvent.OnBackClick -> sendAction(ProfileAction.NavigateBack)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            updateState { ProfileState.Loading }
            runCatching { getProfileUseCase() }
                .onSuccess { profile ->
                    updateState { ProfileState.Content(profile = profile) }
                }
                .onFailure { error ->
                    updateState { ProfileState.Error(error.message) }
                }
        }
    }

    private fun onEditClick() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState {
                ProfileState.Content(
                    profile = state.profile,
                    isEditing = true,
                    editName = state.profile.name
                )
            }
        }
    }

    private fun onNameChanged(name: String) {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.copy(editName = name) }
        }
    }

    private fun onSave() {
        val state = currentState
        if (state is ProfileState.Content && state.isEditing) {
            viewModelScope.launch {
                runCatching { updateProfileUseCase(state.editName) }
                    .onSuccess { profile ->
                        updateState { ProfileState.Content(profile = profile) }
                        sendAction(ProfileAction.ShowMessage("Профиль обновлён"))
                    }
                    .onFailure { error ->
                        sendAction(ProfileAction.ShowMessage(error.message ?: "Ошибка"))
                    }
            }
        }
    }

    private fun onCancelEdit() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.copy(isEditing = false) }
        }
    }

    private fun onLogout() {
        viewModelScope.launch {
            tokenStorage.clearTokens()
            sendAction(ProfileAction.NavigateToLogin)
        }
    }
}
