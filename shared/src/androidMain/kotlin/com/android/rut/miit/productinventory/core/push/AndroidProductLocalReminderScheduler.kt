package com.android.rut.miit.productinventory.core.push

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max

fun Context.scheduleProductLocalReminders(reminders: List<ProductLocalReminder>) {
    val preferences = getSharedPreferences(LOCAL_REMINDER_PREFERENCES, Context.MODE_PRIVATE)
    val scheduledIds = preferences.getStringSet(SCHEDULED_LOCAL_REMINDER_IDS_KEY, emptySet()).orEmpty()
    val plannedIds = reminders.mapTo(mutableSetOf()) { it.id }
    val staleIds = scheduledIds - plannedIds

    staleIds.forEach(::cancelProductLocalReminder)
    reminders
        .filterNot { it.id in scheduledIds }
        .forEach(::scheduleProductLocalReminder)

    preferences.edit()
        .putStringSet(SCHEDULED_LOCAL_REMINDER_IDS_KEY, plannedIds)
        .apply()
}

class ProductLocalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.clearScheduledProductLocalReminders()
            return
        }

        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return

        context.showProductInventoryNotification(
            notificationId = reminderId.hashCode(),
            title = title,
            message = message,
            backendNotificationId = reminderId
        )
    }
}

private fun Context.scheduleProductLocalReminder(reminder: ProductLocalReminder) {
    val alarmManager = getSystemService(AlarmManager::class.java)
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        reminder.triggerAtMillis(),
        reminder.pendingIntent(this)
    )
}

private fun Context.cancelProductLocalReminder(reminderId: String) {
    val alarmManager = getSystemService(AlarmManager::class.java)
    existingPendingIntent(reminderId)?.let(alarmManager::cancel)
}

private fun Context.clearScheduledProductLocalReminders() {
    getSharedPreferences(LOCAL_REMINDER_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .remove(SCHEDULED_LOCAL_REMINDER_IDS_KEY)
        .apply()
}

private fun ProductLocalReminder.pendingIntent(context: Context): PendingIntent =
    context.pendingIntent(
        reminderId = id,
        flags = PendingIntent.FLAG_UPDATE_CURRENT,
        title = title,
        message = message
    )

private fun Context.pendingIntent(
    reminderId: String,
    flags: Int,
    title: String? = null,
    message: String? = null
): PendingIntent =
    PendingIntent.getBroadcast(
        this,
        reminderId.hashCode(),
        Intent(this, ProductLocalReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            title?.let { putExtra(EXTRA_TITLE, it) }
            message?.let { putExtra(EXTRA_MESSAGE, it) }
        },
        flags or PendingIntent.FLAG_IMMUTABLE
    )

private fun Context.existingPendingIntent(reminderId: String): PendingIntent? =
    PendingIntent.getBroadcast(
        this,
        reminderId.hashCode(),
        Intent(this, ProductLocalReminderReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )

private fun ProductLocalReminder.triggerAtMillis(): Long {
    val now = System.currentTimeMillis()
    val minimumTrigger = now + MIN_IMMEDIATE_TRIGGER_DELAY_MS
    val triggerDate = triggerDateIso
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return minimumTrigger
    val scheduledAt = triggerDate
        .atTime(LocalTime.of(9, 0))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    return max(scheduledAt, minimumTrigger)
}

private const val MIN_IMMEDIATE_TRIGGER_DELAY_MS = 5_000L
private const val LOCAL_REMINDER_PREFERENCES = "product_local_reminders"
private const val SCHEDULED_LOCAL_REMINDER_IDS_KEY = "scheduled_local_reminder_ids"
private const val EXTRA_REMINDER_ID = "reminder_id"
private const val EXTRA_TITLE = "title"
private const val EXTRA_MESSAGE = "message"
