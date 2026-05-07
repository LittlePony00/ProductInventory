package com.android.rut.miit.productinventory.feature.notifications.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkAllReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkNotificationReadUseCase
import kotlinx.coroutines.launch

class NotificationListViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllReadUseCase
) : SharedViewModel<NotificationListState, NotificationListEvent, NotificationListAction>(
    initialState = NotificationListState.Loading
) {

    override suspend fun handleEvent(event: NotificationListEvent) {
        when (event) {
            is NotificationListEvent.OnCreate -> loadNotifications()
            is NotificationListEvent.OnRetry -> loadNotifications()
            is NotificationListEvent.OnMarkRead -> markRead(event.notificationId)
            is NotificationListEvent.OnMarkAllRead -> markAllRead()
            is NotificationListEvent.OnBackClick ->
                sendAction(NotificationListAction.NavigateBack)
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            updateState { NotificationListState.Loading }
            runCatching { getNotificationsUseCase() }
                .onSuccess { updateState { NotificationListState.Content(it) } }
                .onFailure { updateState { NotificationListState.Error(it.message) } }
        }
    }

    private fun markRead(notificationId: String) {
        viewModelScope.launch {
            runCatching { markNotificationReadUseCase(notificationId) }
                .onSuccess { loadNotifications() }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            runCatching { markAllReadUseCase() }
                .onSuccess { loadNotifications() }
        }
    }
}
