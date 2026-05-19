package com.android.rut.miit.productinventory.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun Context.isPostNotificationsPermissionGranted(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun Context.showProductInventoryNotification(
    notificationId: Int,
    title: String,
    message: String,
    backendNotificationId: String? = null
) {
    if (!isPostNotificationsPermissionGranted()) return

    ensureProductInventoryNotificationChannel()
    val manager = getSystemService(NotificationManager::class.java)

    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
    val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val platformNotification = android.app.Notification.Builder(this, PRODUCT_REMINDER_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(android.app.Notification.BigTextStyle().bigText(message))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    manager.notify(notificationId, platformNotification)
    backendNotificationId?.let(::markProductInventoryNotificationShown)
}

fun Context.ensureProductInventoryNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(
            PRODUCT_REMINDER_NOTIFICATION_CHANNEL_ID,
            PRODUCT_REMINDER_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
    )
}

fun Context.markProductInventoryNotificationShown(notificationId: String) {
    val preferences = getSharedPreferences(SHOWN_NOTIFICATIONS_PREFERENCES, Context.MODE_PRIVATE)
    val shownIds = preferences.getStringSet(SHOWN_NOTIFICATION_IDS_KEY, emptySet()).orEmpty()
    if (notificationId in shownIds) return

    preferences.edit()
        .putStringSet(SHOWN_NOTIFICATION_IDS_KEY, shownIds + notificationId)
        .apply()
}

const val PRODUCT_REMINDER_NOTIFICATION_CHANNEL_ID = "product_reminders"
private const val PRODUCT_REMINDER_NOTIFICATION_CHANNEL_NAME = "Напоминания о продуктах"
private const val SHOWN_NOTIFICATIONS_PREFERENCES = "shown_notifications"
private const val SHOWN_NOTIFICATION_IDS_KEY = "shown_notification_ids"
