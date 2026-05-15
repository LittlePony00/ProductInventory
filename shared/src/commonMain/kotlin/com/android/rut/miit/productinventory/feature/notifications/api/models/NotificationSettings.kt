package com.android.rut.miit.productinventory.feature.notifications.api.models

data class NotificationSettings(
    val expirationRemindersEnabled: Boolean = true,
    val lowStockRemindersEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val expirationReminderDays: Int = DEFAULT_EXPIRATION_REMINDER_DAYS,
    val updatedAt: String = ""
) {
    companion object {
        const val MIN_EXPIRATION_REMINDER_DAYS = 1
        const val MAX_EXPIRATION_REMINDER_DAYS = 30
        const val DEFAULT_EXPIRATION_REMINDER_DAYS = 3
    }
}
