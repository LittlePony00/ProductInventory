package com.android.rut.miit.productinventory.feature.notifications.data.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponseDto(
    val id: String,
    val title: String,
    val message: String,
    val sentAt: String,
    val isRead: Boolean
)

@Serializable
data class NotificationSettingsResponseDto(
    val expirationRemindersEnabled: Boolean,
    val lowStockRemindersEnabled: Boolean,
    val pushEnabled: Boolean,
    val expirationReminderDays: Int,
    val updatedAt: String
)

@Serializable
data class UpdateNotificationSettingsRequestDto(
    val expirationRemindersEnabled: Boolean? = null,
    val lowStockRemindersEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null,
    val expirationReminderDays: Int? = null
)
