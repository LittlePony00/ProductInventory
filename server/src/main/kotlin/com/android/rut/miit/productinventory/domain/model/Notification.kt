package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val type: NotificationType = NotificationType.GENERAL,
    val title: String,
    val message: String,
    val householdId: UUID? = null,
    val productId: UUID? = null,
    val dedupeKey: String? = null,
    val sentAt: Instant = Instant.now(),
    val isRead: Boolean = false
)

enum class NotificationType {
    GENERAL,
    REMINDER_EXPIRING_SOON,
    REMINDER_LOW_STOCK
}
