package com.android.rut.miit.productinventory.feature.auth.presentation.login

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.auth.api.LoginUseCase
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : SharedViewModel<LoginState, LoginEvent, LoginAction>(
    initialState = LoginState.Input()
) {

    override suspend fun handleEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnEmailChanged -> onEmailChanged(event.email)
            is LoginEvent.OnPasswordChanged -> onPasswordChanged(event.password)
            is LoginEvent.OnLoginClick -> onLoginClick()
            is LoginEvent.OnRegisterClick -> onRegisterClick()
        }
    }

    private fun onEmailChanged(email: String) {
        val current = currentState
        if (current is LoginState.Input) {
            updateState { LoginState.Input(email = email, password = current.password) }
        }
    }

    private fun onPasswordChanged(password: String) {
        val current = currentState
        if (current is LoginState.Input) {
            updateState { LoginState.Input(email = current.email, password = password) }
        }
    }

    private fun onLoginClick() {
        val current = currentState
        if (current !is LoginState.Input) return

        viewModelScope.launch {
            updateState { LoginState.Input(email = current.email, password = current.password, isLoading = true) }
            runCatching { loginUseCase(current.email, current.password) }
                .onSuccess { sendAction(LoginAction.NavigateToHome) }
                .onFailure { error ->
                    updateState { LoginState.Error(error.message) }
                }
        }
    }

    private fun onRegisterClick() {
        sendAction(LoginAction.NavigateToRegister)
    }
}
