package com.android.rut.miit.productinventory.ui.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.rut.miit.productinventory.core.push.showProductInventoryNotification
import com.android.rut.miit.productinventory.core.push.showUnreadProductInventoryNotifications
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class AndroidNotificationPresenterTest {

    @Before
    fun setUp() {
        runCatching { stopKoin() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("shown_notifications", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun `shows unread backend notifications without opening notification screen`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        context.showUnreadProductInventoryNotifications(
            listOf(
                Notification(
                    id = "notification-1",
                    title = "Low stock",
                    message = "Milk is almost out",
                    sentAt = "2026-05-17T12:00:00Z",
                    isRead = false
                )
            )
        )

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `does not show same unread backend notification twice`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = Notification(
            id = "notification-2",
            title = "Expiring soon",
            message = "Yogurt expires tomorrow",
            sentAt = "2026-05-17T12:00:00Z",
            isRead = false
        )

        context.showUnreadProductInventoryNotifications(listOf(notification))
        context.showUnreadProductInventoryNotifications(listOf(notification))

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun `does not mirror notification already displayed from FCM`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = Notification(
            id = "notification-from-fcm",
            title = "New product",
            message = "Milk was added",
            sentAt = "2026-05-17T12:00:00Z",
            isRead = false
        )

        context.showProductInventoryNotification(
            notificationId = 42,
            title = notification.title,
            message = notification.message,
            backendNotificationId = notification.id
        )
        context.showUnreadProductInventoryNotifications(listOf(notification))

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        assertEquals(1, shadowOf(notificationManager).allNotifications.size)
    }
}
