package com.android.rut.miit.productinventory.feature.notifications.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkAllReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkNotificationReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.UpdateNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import kotlinx.coroutines.launch

class NotificationListViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllReadUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase
) : SharedViewModel<NotificationListState, NotificationListEvent, NotificationListAction>(
    initialState = NotificationListState.Loading
) {

    override suspend fun handleEvent(event: NotificationListEvent) {
        when (event) {
            is NotificationListEvent.OnCreate -> loadNotifications()
            is NotificationListEvent.OnRetry -> loadNotifications()
            is NotificationListEvent.OnMarkRead -> markRead(event.notificationId)
            is NotificationListEvent.OnMarkAllRead -> markAllRead()
            is NotificationListEvent.OnExpirationRemindersEnabledChange ->
                saveSettings { copy(expirationRemindersEnabled = event.enabled) }
            is NotificationListEvent.OnLowStockRemindersEnabledChange ->
                saveSettings { copy(lowStockRemindersEnabled = event.enabled) }
            is NotificationListEvent.OnPushEnabledChange ->
                saveSettings { copy(pushEnabled = event.enabled) }
            is NotificationListEvent.OnExpirationReminderDaysChange ->
                saveSettings {
                    copy(
                        expirationReminderDays = event.days.coerceIn(
                            NotificationSettings.MIN_EXPIRATION_REMINDER_DAYS,
                            NotificationSettings.MAX_EXPIRATION_REMINDER_DAYS
                        )
                    )
                }
            is NotificationListEvent.OnBackClick ->
                sendAction(NotificationListAction.NavigateBack)
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            updateState { NotificationListState.Loading }
            runCatching {
                NotificationListState.Content(
                    notifications = getNotificationsUseCase(),
                    settings = getNotificationSettingsUseCase()
                )
            }
                .onSuccess { content -> updateState { content } }
                .onFailure { updateState { NotificationListState.Error(it.message) } }
        }
    }

    private fun markRead(notificationId: String) {
        viewModelScope.launch {
            runCatching { markNotificationReadUseCase(notificationId) }
                .onSuccess { refreshNotifications() }
        }
    }

    private fun markAllRead() {
        viewModelScope.launch {
            runCatching { markAllReadUseCase() }
                .onSuccess { refreshNotifications() }
        }
    }

    private fun refreshNotifications() {
        viewModelScope.launch {
            val currentContent = currentState as? NotificationListState.Content
            runCatching { getNotificationsUseCase() }
                .onSuccess { notifications ->
                    if (currentContent == null) {
                        loadNotifications()
                    } else {
                        updateState {
                            (this as? NotificationListState.Content)?.copy(notifications = notifications) ?: this
                        }
                    }
                }
        }
    }

    private fun saveSettings(reducer: NotificationSettings.() -> NotificationSettings) {
        val content = currentState as? NotificationListState.Content ?: return
        val previous = content.settings
        val requested = previous.reducer()
        updateState {
            content.copy(
                settings = requested,
                isSavingSettings = true,
                settingsError = null
            )
        }
        viewModelScope.launch {
            runCatching {
                updateNotificationSettingsUseCase(
                    expirationRemindersEnabled = requested.expirationRemindersEnabled,
                    lowStockRemindersEnabled = requested.lowStockRemindersEnabled,
                    pushEnabled = requested.pushEnabled,
                    expirationReminderDays = requested.expirationReminderDays
                )
            }
                .onSuccess { saved ->
                    updateState {
                        (this as? NotificationListState.Content)?.copy(
                            settings = saved,
                            isSavingSettings = false,
                            settingsError = null
                        ) ?: this
                    }
                }
                .onFailure { error ->
                    updateState {
                        (this as? NotificationListState.Content)?.copy(
                            settings = previous,
                            isSavingSettings = false,
                            settingsError = error.message
                        ) ?: this
                    }
                }
        }
    }
}
