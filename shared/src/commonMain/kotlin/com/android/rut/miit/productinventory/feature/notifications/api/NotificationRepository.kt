package com.android.rut.miit.productinventory.feature.notifications.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification

interface NotificationRepository {
    suspend fun getNotifications(): List<Notification>
    suspend fun getUnreadNotifications(): List<Notification>
    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead()
}
