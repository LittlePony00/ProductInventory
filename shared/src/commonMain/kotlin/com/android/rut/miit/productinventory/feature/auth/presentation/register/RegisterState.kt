package com.android.rut.miit.productinventory.feature.auth.presentation.register

import com.android.rut.miit.productinventory.common.UiState

sealed class RegisterState : UiState {
    data class Input(
        val email: String = "",
        val password: String = "",
        val name: String = "",
        val isLoading: Boolean = false
    ) : RegisterState()

    data class Error(val message: String?) : RegisterState()
}
