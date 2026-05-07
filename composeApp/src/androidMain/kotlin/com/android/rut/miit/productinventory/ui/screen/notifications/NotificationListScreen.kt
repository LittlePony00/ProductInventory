package com.android.rut.miit.productinventory.ui.screen.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.presentation.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onBack: () -> Unit,
    viewModel: NotificationListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

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
                title = { Text("Уведомления") },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(NotificationListEvent.OnBackClick) }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(NotificationListEvent.OnMarkAllRead) }) {
                        Text("Прочитать все")
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
                if (s.notifications.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Нет уведомлений", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        Text(s.message ?: "Ошибка загрузки")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(NotificationListEvent.OnRetry) }) {
                            Text("Повторить")
                        }
                    }
                }
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
                        Text("Прочитано", style = MaterialTheme.typography.labelSmall)
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
