package com.android.rut.miit.productinventory.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.core.R
import com.android.rut.miit.productinventory.feature.auth.presentation.register.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is RegisterAction.NavigateToHome -> onNavigateToHome()
                is RegisterAction.NavigateBack -> onNavigateBack()
            }
        }
    }

    RegisterContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun RegisterContent(
    state: RegisterState,
    onEvent: (RegisterEvent) -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (state) {
                is RegisterState.Input -> {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onEvent(RegisterEvent.OnNameChanged(it)) },
                        label = { Text(stringResource(R.string.register_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(RegisterEvent.OnEmailChanged(it)) },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(RegisterEvent.OnPasswordChanged(it)) },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEvent(RegisterEvent.OnRegisterClick) },
                        enabled = !state.isLoading
                                && state.email.isNotBlank()
                                && state.password.isNotBlank()
                                && state.name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.register_button))
                        }
                    }
                }

                is RegisterState.Error -> {
                    Text(
                        text = state.message ?: stringResource(R.string.error_generic),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onEvent(RegisterEvent.OnRegisterClick) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onEvent(RegisterEvent.OnBackToLogin) }) {
                Text(stringResource(R.string.register_has_account))
            }
        }
    }
}
