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
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.auth.presentation.login.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is LoginAction.NavigateToHome -> onNavigateToHome()
                is LoginAction.NavigateToRegister -> onNavigateToRegister()
            }
        }
    }

    LoginContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun LoginContent(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit
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
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (state) {
                is LoginState.Input -> {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { onEvent(LoginEvent.OnEmailChanged(it)) },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(LoginEvent.OnPasswordChanged(it)) },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEvent(LoginEvent.OnLoginClick) },
                        enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.login_button))
                        }
                    }
                }

                is LoginState.Error -> {
                    Text(
                        text = state.message ?: stringResource(R.string.error_generic),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onEvent(LoginEvent.OnLoginClick) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onEvent(LoginEvent.OnRegisterClick) }) {
                Text(stringResource(R.string.login_no_account))
            }
        }
    }
}
