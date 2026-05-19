package com.android.rut.miit.productinventory.feature.auth.presentation.register

import com.android.rut.miit.productinventory.common.UiEvent

sealed class RegisterEvent : UiEvent {
    data class OnEmailChanged(val email: String) : RegisterEvent()
    data class OnPasswordChanged(val password: String) : RegisterEvent()
    data class OnNameChanged(val name: String) : RegisterEvent()
    data object OnRegisterClick : RegisterEvent()
    data object OnBackToLogin : RegisterEvent()
}
