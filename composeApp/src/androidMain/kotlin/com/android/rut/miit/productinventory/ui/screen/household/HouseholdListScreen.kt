package com.android.rut.miit.productinventory.ui.screen.household

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.presentation.list.*
import com.android.rut.miit.productinventory.ui.design.ScreenError
import com.android.rut.miit.productinventory.ui.design.ScreenLoading
import com.android.rut.miit.productinventory.ui.design.ScreenMessage
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.launch

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
    var joinError by remember { mutableStateOf<String?>(null) }
    var inviteCodeDialog by remember { mutableStateOf<InviteCodeDialogState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.onEvent(HouseholdListEvent.OnCreate) }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is HouseholdListAction.OpenHousehold -> onNavigateToHousehold(action.householdId)
                is HouseholdListAction.ShowCreateDialog -> showCreateDialog = true
                is HouseholdListAction.ShowJoinDialog -> showJoinDialog = true
                is HouseholdListAction.CloseJoinDialog -> {
                    showJoinDialog = false
                    joinCode = ""
                    joinError = null
                }
                is HouseholdListAction.ShowInviteCode -> {
                    inviteCodeDialog = InviteCodeDialogState(action.code, action.expiresAt)
                }
                is HouseholdListAction.ShowMessage -> {
                    if (showJoinDialog) {
                        joinError = action.message
                    }
                    snackbarHostState.showSnackbar(action.message)
                }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val hasHouseholds = (state as? HouseholdListState.Content)?.households?.isNotEmpty() == true
            if (hasHouseholds) {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdClick) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("#  ${stringResource(R.string.join)}", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdClick) }
                ) {
                    Text("+  ${stringResource(R.string.add)}", style = MaterialTheme.typography.labelMedium)
                }
            }
            }
        }
    ) { padding ->
        when (val s = state) {
            is HouseholdListState.Loading -> {
                ScreenLoading(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is HouseholdListState.Content -> {
                if (s.households.isEmpty()) {
                    ScreenMessage(
                        title = stringResource(R.string.households_empty),
                        message = stringResource(R.string.households_empty_hint),
                        iconText = "+",
                        primaryActionLabel = stringResource(R.string.household_create),
                        onPrimaryAction = { viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdClick) },
                        secondaryActionLabel = stringResource(R.string.household_join),
                        onSecondaryAction = { viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdClick) },
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.households) { household ->
                            HouseholdCard(
                                household = household,
                                onOpen = {
                                    viewModel.onEvent(HouseholdListEvent.OnHouseholdClick(household.id))
                                },
                                onGenerateInvite = {
                                    viewModel.onEvent(HouseholdListEvent.OnGenerateInviteCodeClick(household.id))
                                }
                            )
                        }
                    }
                }
            }
            is HouseholdListState.Error -> {
                ScreenError(
                    message = s.message ?: stringResource(R.string.error_loading),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.onEvent(HouseholdListEvent.OnRetry) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
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
            onDismissRequest = {
                showJoinDialog = false
                joinCode = ""
                joinError = null
            },
            title = { Text(stringResource(R.string.household_join_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = {
                            joinCode = it
                            joinError = null
                        },
                        label = { Text(stringResource(R.string.household_invite_code_label)) },
                        isError = joinError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    joinError?.let { error ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        joinError = null
                        viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdConfirm(joinCode))
                    },
                    enabled = joinCode.isNotBlank()
                ) { Text(stringResource(R.string.household_join)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showJoinDialog = false
                    joinCode = ""
                    joinError = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    inviteCodeDialog?.let { invite ->
        val inviteCopiedMessage = stringResource(R.string.household_invite_code_copied)
        AlertDialog(
            onDismissRequest = { inviteCodeDialog = null },
            title = { Text(stringResource(R.string.household_invite_code_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(
                            invite.code,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.household_invite_code_expires, invite.expiresAt.take(16)))
                    Text(
                        stringResource(R.string.household_invite_code_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText("invite code", invite.code).toClipEntry()
                            )
                            snackbarHostState.showSnackbar(message = inviteCopiedMessage)
                        }
                    }
                ) {
                    Text(stringResource(R.string.copy))
                }
            },
            dismissButton = {
                TextButton(onClick = { inviteCodeDialog = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun HouseholdCard(
    household: Household,
    onOpen: () -> Unit,
    onGenerateInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onGenerateInvite) {
                    Text(stringResource(R.string.household_invite))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.household_open))
                }
            }
        }
    }
}

private data class InviteCodeDialogState(
    val code: String,
    val expiresAt: String
)
