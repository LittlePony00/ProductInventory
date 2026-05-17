package com.android.rut.miit.productinventory.ui.notification

import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest
import androidx.lifecycle.Lifecycle
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AndroidNotificationBootstrapTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        runCatching { stopKoin() }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun `authenticated bootstrap registers token and mirrors unread notification`() = runTest {
        val repository = FakeNotificationRepository(
            notifications = listOf(
                Notification(
                    id = "bootstrap-notification-1",
                    title = "Low stock",
                    message = "Milk is almost out",
                    sentAt = "2026-05-17T12:00:00Z",
                    isRead = false
                )
            )
        )
        val registrar = RecordingDeviceTokenRegistrar()

        compose.setContent {
            MaterialTheme {
                AndroidNotificationBootstrap(
                    isAuthenticated = true,
                    getNotificationsUseCase = GetNotificationsUseCase(repository),
                    getNotificationSettingsUseCase = GetNotificationSettingsUseCase(repository),
                    deviceTokenRegistrar = registrar,
                    syncInterval = 10.milliseconds
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            val notificationManager = ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(NotificationManager::class.java)
            registrar.registerCurrentTokenCalls == 1 &&
                shadowOf(notificationManager).allNotifications.isNotEmpty()
        }

        val notificationManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(NotificationManager::class.java)
        assertEquals(1, registrar.registerCurrentTokenCalls)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `polling stops when host is not resumed`() {
        val first = Notification(
            id = "bootstrap-notification-foreground",
            title = "Low stock",
            message = "Milk is almost out",
            sentAt = "2026-05-17T12:00:00Z",
            isRead = false
        )
        val second = Notification(
            id = "bootstrap-notification-background",
            title = "New product",
            message = "Bread was added",
            sentAt = "2026-05-17T12:01:00Z",
            isRead = false
        )
        val repository = FakeNotificationRepository(notifications = listOf(first))
        val registrar = RecordingDeviceTokenRegistrar()

        compose.setContent {
            MaterialTheme {
                AndroidNotificationBootstrap(
                    isAuthenticated = true,
                    getNotificationsUseCase = GetNotificationsUseCase(repository),
                    getNotificationSettingsUseCase = GetNotificationSettingsUseCase(repository),
                    deviceTokenRegistrar = registrar,
                    syncInterval = 10.milliseconds
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            val notificationManager = ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(NotificationManager::class.java)
            shadowOf(notificationManager).allNotifications.size == 1
        }

        repository.notifications = listOf(first, second)
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        Thread.sleep(200)

        val notificationManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    private class RecordingDeviceTokenRegistrar : DeviceTokenRegistrar {
        var registerCurrentTokenCalls = 0
            private set

        override suspend fun registerCurrentToken() {
            registerCurrentTokenCalls += 1
        }

        override suspend fun registerToken(token: String) = Unit
    }

    private class FakeNotificationRepository(
        var notifications: List<Notification> = emptyList(),
        private val settings: NotificationSettings = NotificationSettings(pushEnabled = true)
    ) : NotificationRepository {
        override suspend fun getNotifications(): List<Notification> = notifications

        override suspend fun getUnreadNotifications(): List<Notification> = notifications.filterNot { it.isRead }

        override suspend fun markAsRead(notificationId: String) = Unit

        override suspend fun markAllAsRead() = Unit

        override suspend fun getSettings(): NotificationSettings = settings

        override suspend fun updateSettings(
            expirationRemindersEnabled: Boolean?,
            lowStockRemindersEnabled: Boolean?,
            pushEnabled: Boolean?,
            expirationReminderDays: Int?
        ): NotificationSettings = settings
    }
}
