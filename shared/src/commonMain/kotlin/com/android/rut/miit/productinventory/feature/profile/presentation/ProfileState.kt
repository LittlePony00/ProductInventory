package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile

sealed class ProfileState : UiState {
    data object Loading : ProfileState()
    data class Content(
        val profile: UserProfile,
        val isEditing: Boolean = false,
        val editName: String = ""
    ) : ProfileState()
    data class Error(val message: String?) : ProfileState()
}
