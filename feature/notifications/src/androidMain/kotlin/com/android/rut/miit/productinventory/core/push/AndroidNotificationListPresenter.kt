package com.android.rut.miit.productinventory.core.push

import android.content.Context
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification

fun Context.showUnreadProductInventoryNotifications(notifications: List<Notification>) {
    val unread = notifications.filterNot { it.isRead }
    if (unread.isEmpty()) return

    val preferences = getSharedPreferences(SHOWN_NOTIFICATIONS_PREFERENCES, Context.MODE_PRIVATE)
    val shownIds = preferences.getStringSet(SHOWN_NOTIFICATION_IDS_KEY, emptySet()).orEmpty()
    val pending = unread.filterNot { it.id in shownIds }
    if (pending.isEmpty()) return

    pending.forEach { notification ->
        showProductInventoryNotification(
            notificationId = notification.id.hashCode(),
            title = notification.title,
            message = notification.message,
            backendNotificationId = notification.id
        )
    }
}

private const val SHOWN_NOTIFICATIONS_PREFERENCES = "shown_notifications"
private const val SHOWN_NOTIFICATION_IDS_KEY = "shown_notification_ids"
