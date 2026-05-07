package com.android.rut.miit.productinventory.ui.screen.household

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.presentation.list.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdListScreen(
    onNavigateToHousehold: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HouseholdListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.onEvent(HouseholdListEvent.OnCreate) }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is HouseholdListAction.OpenHousehold -> onNavigateToHousehold(action.householdId)
                is HouseholdListAction.ShowCreateDialog -> showCreateDialog = true
                is HouseholdListAction.ShowJoinDialog -> showJoinDialog = true
                is HouseholdListAction.ShowMessage -> {}
                is HouseholdListAction.OpenProfile -> onNavigateToProfile()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои домохозяйства") },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(HouseholdListEvent.OnProfileClick) }) {
                        Text("Профиль")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdClick) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("↗", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdClick) }
                ) {
                    Text("+", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
    ) { padding ->
        when (val s = state) {
            is HouseholdListState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HouseholdListState.Content -> {
                if (s.households.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Нет домохозяйств", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Создайте новое или присоединитесь по коду",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.households) { household ->
                            HouseholdCard(household) {
                                viewModel.onEvent(HouseholdListEvent.OnHouseholdClick(household.id))
                            }
                        }
                    }
                }
            }
            is HouseholdListState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message ?: "Ошибка загрузки")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(HouseholdListEvent.OnRetry) }) {
                            Text("Повторить")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; createName = "" },
            title = { Text("Новое домохозяйство") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdConfirm(createName))
                        showCreateDialog = false
                        createName = ""
                    },
                    enabled = createName.isNotBlank()
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; createName = "" }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; joinCode = "" },
            title = { Text("Присоединиться") },
            text = {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    label = { Text("Код приглашения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdConfirm(joinCode))
                        showJoinDialog = false
                        joinCode = ""
                    },
                    enabled = joinCode.isNotBlank()
                ) { Text("Вступить") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false; joinCode = "" }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun HouseholdCard(household: Household, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(household.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Создано: ${household.createdAt.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
