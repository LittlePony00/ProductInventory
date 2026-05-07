package com.android.rut.miit.productinventory.feature.auth.presentation.login

import com.android.rut.miit.productinventory.common.UiEvent

sealed class LoginEvent : UiEvent {
    data class OnEmailChanged(val email: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()
    data object OnLoginClick : LoginEvent()
    data object OnRegisterClick : LoginEvent()
}
