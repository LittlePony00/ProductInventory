package com.android.rut.miit.productinventory.feature.notifications.data.mappers

import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.notifications.data.models.NotificationResponseDto
import com.android.rut.miit.productinventory.feature.notifications.data.models.NotificationSettingsResponseDto

fun NotificationResponseDto.toDomain() = Notification(
    id = id,
    title = title,
    message = message,
    sentAt = sentAt,
    isRead = isRead
)

fun NotificationSettingsResponseDto.toDomain() = NotificationSettings(
    expirationRemindersEnabled = expirationRemindersEnabled,
    lowStockRemindersEnabled = lowStockRemindersEnabled,
    pushEnabled = pushEnabled,
    expirationReminderDays = expirationReminderDays,
    updatedAt = updatedAt
)
