package com.android.rut.miit.productinventory.feature.notifications.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings

interface NotificationRepository {
    suspend fun getNotifications(): List<Notification>
    suspend fun getUnreadNotifications(): List<Notification>
    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead()
    suspend fun getSettings(): NotificationSettings
    suspend fun updateSettings(
        expirationRemindersEnabled: Boolean?,
        lowStockRemindersEnabled: Boolean?,
        pushEnabled: Boolean?,
        expirationReminderDays: Int?
    ): NotificationSettings
}
