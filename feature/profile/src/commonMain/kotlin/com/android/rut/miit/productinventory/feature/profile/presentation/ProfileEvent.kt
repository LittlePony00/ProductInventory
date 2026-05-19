package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.common.UiEvent

sealed class ProfileEvent : UiEvent {
    data object OnCreate : ProfileEvent()
    data object OnRetry : ProfileEvent()
    data object OnEditClick : ProfileEvent()
    data class OnNameChanged(val name: String) : ProfileEvent()
    data object OnSaveClick : ProfileEvent()
    data object OnCancelEdit : ProfileEvent()
    data object OnLogoutClick : ProfileEvent()
    data object OnBackClick : ProfileEvent()
}
