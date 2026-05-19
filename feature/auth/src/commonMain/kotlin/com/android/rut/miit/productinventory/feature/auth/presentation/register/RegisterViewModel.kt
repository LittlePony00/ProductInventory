package com.android.rut.miit.productinventory.feature.auth.presentation.register

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.auth.api.RegisterUseCase
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : SharedViewModel<RegisterState, RegisterEvent, RegisterAction>(
    initialState = RegisterState.Input()
) {

    override suspend fun handleEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.OnEmailChanged -> onEmailChanged(event.email)
            is RegisterEvent.OnPasswordChanged -> onPasswordChanged(event.password)
            is RegisterEvent.OnNameChanged -> onNameChanged(event.name)
            is RegisterEvent.OnRegisterClick -> onRegisterClick()
            is RegisterEvent.OnBackToLogin -> sendAction(RegisterAction.NavigateBack)
        }
    }

    private fun onEmailChanged(email: String) {
        val current = currentState as? RegisterState.Input ?: return
        updateState { RegisterState.Input(email = email, password = current.password, name = current.name) }
    }

    private fun onPasswordChanged(password: String) {
        val current = currentState as? RegisterState.Input ?: return
        updateState { RegisterState.Input(email = current.email, password = password, name = current.name) }
    }

    private fun onNameChanged(name: String) {
        val current = currentState as? RegisterState.Input ?: return
        updateState { RegisterState.Input(email = current.email, password = current.password, name = name) }
    }

    private fun onRegisterClick() {
        val current = currentState as? RegisterState.Input ?: return

        viewModelScope.launch {
            updateState { RegisterState.Input(current.email, current.password, current.name, isLoading = true) }
            runCatching { registerUseCase(current.email, current.password, current.name) }
                .onSuccess { sendAction(RegisterAction.NavigateToHome) }
                .onFailure { error -> updateState { RegisterState.Error(error.message) } }
        }
    }
}
