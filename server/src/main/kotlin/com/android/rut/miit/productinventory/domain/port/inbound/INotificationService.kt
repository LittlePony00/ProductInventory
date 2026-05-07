package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Notification
import java.util.UUID

interface INotificationService {
    fun getNotifications(userId: UUID): List<Notification>
    fun getUnreadNotifications(userId: UUID): List<Notification>
    fun markAsRead(userId: UUID, notificationId: UUID)
    fun markAllAsRead(userId: UUID)
}
