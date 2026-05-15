package com.android.rut.miit.productinventory.ui.screen.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.notifications.presentation.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    viewModel: NotificationListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isPushPermissionGranted by remember {
        mutableStateOf(context.isPostNotificationsPermissionGranted())
    }
    var pendingPushEnable by remember { mutableStateOf(false) }
    var showPushPermissionDenied by remember { mutableStateOf(false) }
    val pushPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPushPermissionGranted = granted || context.isPostNotificationsPermissionGranted()
        if (pendingPushEnable) {
            pendingPushEnable = false
            if (isPushPermissionGranted) {
                showPushPermissionDenied = false
                viewModel.onEvent(NotificationListEvent.OnPushEnabledChange(true))
            } else {
                showPushPermissionDenied = true
            }
        }
    }

    fun requestPushPermission() {
        if (context.isPostNotificationsPermissionGranted()) {
            isPushPermissionGranted = true
            showPushPermissionDenied = false
            if (pendingPushEnable) {
                pendingPushEnable = false
                viewModel.onEvent(NotificationListEvent.OnPushEnabledChange(true))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pushPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { viewModel.onEvent(NotificationListEvent.OnCreate) }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is NotificationListAction.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(NotificationListEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(NotificationListEvent.OnMarkAllRead) }) {
                        Text(stringResource(R.string.notifications_mark_all_read))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is NotificationListState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is NotificationListState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        NotificationSettingsCard(
                            settings = s.settings,
                            isSaving = s.isSavingSettings,
                            error = s.settingsError,
                            onExpirationEnabledChange = {
                                viewModel.onEvent(NotificationListEvent.OnExpirationRemindersEnabledChange(it))
                            },
                            onLowStockEnabledChange = {
                                viewModel.onEvent(NotificationListEvent.OnLowStockRemindersEnabledChange(it))
                            },
                            onPushEnabledChange = {
                                if (!it) {
                                    pendingPushEnable = false
                                    showPushPermissionDenied = false
                                    viewModel.onEvent(NotificationListEvent.OnPushEnabledChange(false))
                                } else if (isPushPermissionGranted) {
                                    showPushPermissionDenied = false
                                    viewModel.onEvent(NotificationListEvent.OnPushEnabledChange(true))
                                } else {
                                    pendingPushEnable = true
                                    requestPushPermission()
                                }
                            },
                            onExpirationDaysChange = {
                                viewModel.onEvent(NotificationListEvent.OnExpirationReminderDaysChange(it))
                            },
                            isPushPermissionRequired = s.settings.pushEnabled && !isPushPermissionGranted,
                            showPushPermissionDenied = showPushPermissionDenied,
                            onRequestPushPermission = {
                                pendingPushEnable = s.settings.pushEnabled
                                requestPushPermission()
                            },
                        )
                    }
                    if (s.notifications.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.notifications_empty),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else {
                        items(s.notifications) { notification ->
                            NotificationCard(notification) {
                                viewModel.onEvent(NotificationListEvent.OnMarkRead(notification.id))
                            }
                        }
                    }
                }
            }
            is NotificationListState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message ?: stringResource(R.string.error_loading))
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(NotificationListEvent.OnRetry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    settings: NotificationSettings,
    isSaving: Boolean,
    error: String?,
    isPushPermissionRequired: Boolean,
    showPushPermissionDenied: Boolean,
    onExpirationEnabledChange: (Boolean) -> Unit,
    onLowStockEnabledChange: (Boolean) -> Unit,
    onPushEnabledChange: (Boolean) -> Unit,
    onExpirationDaysChange: (Int) -> Unit,
    onRequestPushPermission: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.notifications_settings_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isSaving) {
                    Text(
                        stringResource(R.string.notifications_settings_saving),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            NotificationSwitchRow(
                title = stringResource(R.string.notifications_settings_expiration),
                checked = settings.expirationRemindersEnabled,
                enabled = !isSaving,
                onCheckedChange = onExpirationEnabledChange
            )
            ExpirationDaysRow(
                days = settings.expirationReminderDays,
                enabled = !isSaving && settings.expirationRemindersEnabled,
                onDaysChange = onExpirationDaysChange
            )
            NotificationSwitchRow(
                title = stringResource(R.string.notifications_settings_low_stock),
                checked = settings.lowStockRemindersEnabled,
                enabled = !isSaving,
                onCheckedChange = onLowStockEnabledChange
            )
            NotificationSwitchRow(
                title = stringResource(R.string.notifications_settings_push),
                checked = settings.pushEnabled,
                enabled = !isSaving,
                onCheckedChange = onPushEnabledChange
            )
            if (isPushPermissionRequired || showPushPermissionDenied) {
                Text(
                    text = if (showPushPermissionDenied) {
                        stringResource(R.string.notifications_push_permission_denied)
                    } else {
                        stringResource(R.string.notifications_push_permission_required)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showPushPermissionDenied) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                OutlinedButton(
                    onClick = onRequestPushPermission,
                    enabled = !isSaving
                ) {
                    Text(stringResource(R.string.notifications_push_permission_grant))
                }
            }
            if (error != null) {
                Text(
                    text = error.ifBlank { stringResource(R.string.notifications_settings_error) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun Context.isPostNotificationsPermissionGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

@Composable
private fun NotificationSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ExpirationDaysRow(
    days: Int,
    enabled: Boolean,
    onDaysChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.notifications_settings_expiration_days, days),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onDaysChange(days - 1) },
                enabled = enabled && days > NotificationSettings.MIN_EXPIRATION_REMINDER_DAYS
            ) {
                Text("-")
            }
            OutlinedButton(
                onClick = { onDaysChange(days + 1) },
                enabled = enabled && days < NotificationSettings.MAX_EXPIRATION_REMINDER_DAYS
            ) {
                Text("+")
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: Notification, onMarkRead: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                if (!notification.isRead) {
                    TextButton(onClick = onMarkRead) {
                        Text(stringResource(R.string.notifications_mark_read), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(notification.message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                notification.sentAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
