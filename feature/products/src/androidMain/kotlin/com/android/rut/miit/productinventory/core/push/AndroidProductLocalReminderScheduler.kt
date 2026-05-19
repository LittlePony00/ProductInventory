package com.android.rut.miit.productinventory.core.push

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminder
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminderPlanner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
        .putString(PLANNED_LOCAL_REMINDERS_KEY, localReminderJson.encodeToString(reminders))
        .apply()
}

class ProductLocalReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val bootRescheduler: AndroidProductLocalReminderRescheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            CoroutineScope(SupervisorJob() + productLocalReminderBootDispatcher).launch {
                val restoredPersistedReminders = runCatching {
                    appContext.reschedulePersistedProductLocalRemindersAfterBoot()
                }.onFailure { error ->
                    Log.w(TAG, "Failed to reschedule persisted product reminders after boot", error)
                }.getOrDefault(false)

                if (!restoredPersistedReminders) {
                    runCatching { appContext.rescheduleProductLocalRemindersAfterBoot(bootRescheduler) }
                        .onFailure { error ->
                            Log.w(TAG, "Failed to rebuild product reminders after boot", error)
                        }
                }
                pendingResult.finish()
            }
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

internal suspend fun Context.rescheduleProductLocalRemindersAfterBoot(
    bootRescheduler: AndroidProductLocalReminderRescheduler
) {
    clearScheduledProductLocalReminders()
    bootRescheduler.reschedule(applicationContext)
}

internal fun Context.reschedulePersistedProductLocalRemindersAfterBoot(): Boolean {
    val reminders = loadPlannedProductLocalReminders()
    if (reminders.isEmpty()) return false

    clearScheduledProductLocalReminders()
    scheduleProductLocalReminders(reminders)
    Log.i(TAG, "Rescheduled ${reminders.size} persisted product reminders after boot")
    return true
}

class AndroidProductLocalReminderRescheduler(
    private val householdLocalDataSource: HouseholdLocalDataSource,
    private val productLocalDataSource: ProductLocalDataSource,
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase
) {
    private val planner = ProductLocalReminderPlanner()

    suspend fun reschedule(context: Context) {
        val settings = runCatching { getNotificationSettingsUseCase() }
            .getOrDefault(NotificationSettings())
        val products = householdLocalDataSource.getHouseholds()
            .flatMap { household -> productLocalDataSource.getProducts(household.id) }
        context.scheduleProductLocalReminders(planner.plan(products, settings))
    }
}

private fun Context.scheduleProductLocalReminder(reminder: ProductLocalReminder) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        reminder.triggerAtMillis(),
        reminder.pendingIntent(this)
    )
}

private fun Context.cancelProductLocalReminder(reminderId: String) {
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    existingPendingIntent(reminderId)?.let(alarmManager::cancel)
}

private fun Context.clearScheduledProductLocalReminders() {
    getSharedPreferences(LOCAL_REMINDER_PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .remove(SCHEDULED_LOCAL_REMINDER_IDS_KEY)
        .apply()
}

private fun Context.loadPlannedProductLocalReminders(): List<ProductLocalReminder> =
    getSharedPreferences(LOCAL_REMINDER_PREFERENCES, Context.MODE_PRIVATE)
        .getString(PLANNED_LOCAL_REMINDERS_KEY, null)
        ?.let { payload -> localReminderJson.decodeFromString<List<ProductLocalReminder>>(payload) }
        .orEmpty()

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
private const val PLANNED_LOCAL_REMINDERS_KEY = "planned_local_reminders"
private const val EXTRA_REMINDER_ID = "reminder_id"
private const val EXTRA_TITLE = "title"
private const val EXTRA_MESSAGE = "message"
private const val TAG = "ProductReminderReceiver"

private val localReminderJson = Json {
    ignoreUnknownKeys = true
}

internal var productLocalReminderBootDispatcher: CoroutineDispatcher = Dispatchers.IO
