package com.android.rut.miit.productinventory.feature.notifications.presentation

import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkAllReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkNotificationReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.UpdateNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class NotificationListViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads notifications and notification settings`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeNotificationRepository(
                notifications = listOf(notification("n1")),
                settings = NotificationSettings(pushEnabled = false, expirationReminderDays = 5)
            )
            val viewModel = viewModel(repository)

            viewModel.onEvent(NotificationListEvent.OnCreate)
            advanceUntilIdle()

            val state = assertIs<NotificationListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("n1"), state.notifications.map { it.id })
            assertEquals(false, state.settings.pushEnabled)
            assertEquals(5, state.settings.expirationReminderDays)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `push setting change persists and updates state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeNotificationRepository(settings = NotificationSettings(pushEnabled = true))
            val viewModel = viewModel(repository)
            viewModel.onEvent(NotificationListEvent.OnCreate)
            advanceUntilIdle()

            viewModel.onEvent(NotificationListEvent.OnPushEnabledChange(false))
            advanceUntilIdle()

            val state = assertIs<NotificationListState.Content>(viewModel.viewState.value)
            assertEquals(false, state.settings.pushEnabled)
            assertEquals(false, repository.settings.pushEnabled)
            assertEquals(1, repository.updateCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `failed setting update restores previous state and shows error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeNotificationRepository(
                settings = NotificationSettings(lowStockRemindersEnabled = true),
                failUpdates = true
            )
            val viewModel = viewModel(repository)
            viewModel.onEvent(NotificationListEvent.OnCreate)
            advanceUntilIdle()

            viewModel.onEvent(NotificationListEvent.OnLowStockRemindersEnabledChange(false))
            advanceUntilIdle()

            val state = assertIs<NotificationListState.Content>(viewModel.viewState.value)
            assertTrue(state.settings.lowStockRemindersEnabled)
            assertEquals("network down", state.settingsError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: NotificationRepository): NotificationListViewModel =
        NotificationListViewModel(
            getNotificationsUseCase = GetNotificationsUseCase(repository),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(repository),
            markNotificationReadUseCase = MarkNotificationReadUseCase(repository),
            markAllReadUseCase = MarkAllReadUseCase(repository),
            updateNotificationSettingsUseCase = UpdateNotificationSettingsUseCase(repository)
        )

    private class FakeNotificationRepository(
        var notifications: List<Notification> = emptyList(),
        var settings: NotificationSettings = NotificationSettings(),
        private val failUpdates: Boolean = false
    ) : NotificationRepository {
        var updateCalls = 0

        override suspend fun getNotifications(): List<Notification> = notifications
        override suspend fun getUnreadNotifications(): List<Notification> = notifications.filterNot { it.isRead }
        override suspend fun markAsRead(notificationId: String) {
            notifications = notifications.map { notification ->
                if (notification.id == notificationId) notification.copy(isRead = true) else notification
            }
        }

        override suspend fun markAllAsRead() {
            notifications = notifications.map { it.copy(isRead = true) }
        }

        override suspend fun getSettings(): NotificationSettings = settings

        override suspend fun updateSettings(
            expirationRemindersEnabled: Boolean?,
            lowStockRemindersEnabled: Boolean?,
            pushEnabled: Boolean?,
            expirationReminderDays: Int?
        ): NotificationSettings {
            updateCalls += 1
            if (failUpdates) error("network down")
            settings = settings.copy(
                expirationRemindersEnabled = expirationRemindersEnabled ?: settings.expirationRemindersEnabled,
                lowStockRemindersEnabled = lowStockRemindersEnabled ?: settings.lowStockRemindersEnabled,
                pushEnabled = pushEnabled ?: settings.pushEnabled,
                expirationReminderDays = expirationReminderDays ?: settings.expirationReminderDays
            )
            return settings
        }
    }

    private fun notification(id: String): Notification =
        Notification(
            id = id,
            title = "Title",
            message = "Message",
            sentAt = "2026-05-14T00:00:00Z",
            isRead = false
        )
}
