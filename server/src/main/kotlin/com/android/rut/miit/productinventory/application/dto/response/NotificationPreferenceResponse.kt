package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import java.time.Instant
import java.util.UUID

data class NotificationSettingsResponse(
    val expirationRemindersEnabled: Boolean,
    val lowStockRemindersEnabled: Boolean,
    val pushEnabled: Boolean,
    val expirationReminderDays: Int,
    val updatedAt: Instant
)

data class NotificationDeviceTokenResponse(
    val id: UUID,
    val platform: NotificationPlatform,
    val active: Boolean,
    val createdAt: Instant,
    val lastSeenAt: Instant
)
