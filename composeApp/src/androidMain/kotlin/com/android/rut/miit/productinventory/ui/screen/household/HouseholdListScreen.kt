package com.android.rut.miit.productinventory.ui.screen.household

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
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
                title = { Text(stringResource(R.string.households_title)) },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(HouseholdListEvent.OnProfileClick) }) {
                        Text(stringResource(R.string.profile_title))
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
                    Text(stringResource(R.string.join), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdClick) }
                ) {
                    Text(stringResource(R.string.add), style = MaterialTheme.typography.labelMedium)
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
                            Text(stringResource(R.string.households_empty), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.households_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        Text(s.message ?: stringResource(R.string.error_loading))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(HouseholdListEvent.OnRetry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; createName = "" },
            title = { Text(stringResource(R.string.household_new_title)) },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text(stringResource(R.string.household_name_label)) },
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
                ) { Text(stringResource(R.string.household_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; createName = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; joinCode = "" },
            title = { Text(stringResource(R.string.household_join_title)) },
            text = {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    label = { Text(stringResource(R.string.household_invite_code_label)) },
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
                ) { Text(stringResource(R.string.household_join)) }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false; joinCode = "" }) {
                    Text(stringResource(R.string.cancel))
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
                stringResource(R.string.households_created_at, household.createdAt.take(10)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
