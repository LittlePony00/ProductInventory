package com.android.rut.miit.productinventory.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.feature.profile.presentation.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEvent(ProfileEvent.OnCreate) }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is ProfileAction.NavigateToLogin -> onNavigateToLogin()
                is ProfileAction.NavigateBack -> onBack()
                is ProfileAction.ShowMessage -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnBackClick) }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is ProfileState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileState.Content -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Email", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(s.profile.email, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Имя", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (s.isEditing) {
                                OutlinedTextField(
                                    value = s.editName,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnNameChanged(it)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnCancelEdit) }) {
                                        Text("Отмена")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { viewModel.onEvent(ProfileEvent.OnSaveClick) }) {
                                        Text("Сохранить")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(s.profile.name, style = MaterialTheme.typography.bodyLarge)
                                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnEditClick) }) {
                                        Text("Изменить")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick = { viewModel.onEvent(ProfileEvent.OnLogoutClick) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Выйти из аккаунта")
                    }
                }
            }
            is ProfileState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message ?: "Ошибка загрузки")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(ProfileEvent.OnRetry) }) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }
}
