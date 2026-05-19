package com.android.rut.miit.productinventory.feature.auth.presentation.login

import com.android.rut.miit.productinventory.common.UiState

sealed class LoginState : UiState {
    data class Input(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false
    ) : LoginState()

    data class Error(val message: String?) : LoginState()
}
