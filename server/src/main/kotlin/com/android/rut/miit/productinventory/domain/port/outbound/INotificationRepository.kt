package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Notification
import java.util.UUID

interface INotificationRepository {
    fun findByUserId(userId: UUID): List<Notification>
    fun findUnreadByUserId(userId: UUID): List<Notification>
    fun existsByUserIdAndDedupeKey(userId: UUID, dedupeKey: String): Boolean
    fun save(notification: Notification): Notification
    fun markAsRead(id: UUID, userId: UUID)
    fun markAllAsRead(userId: UUID)
}
