package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class NotificationSettings(
    val userId: UUID,
    val expirationRemindersEnabled: Boolean = true,
    val lowStockRemindersEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val expirationReminderDays: Int = DEFAULT_EXPIRATION_REMINDER_DAYS,
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(expirationReminderDays in 1..30) { "Expiration reminder days must be between 1 and 30" }
    }

    companion object {
        const val DEFAULT_EXPIRATION_REMINDER_DAYS = 3
    }
}
