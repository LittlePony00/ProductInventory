package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.port.inbound.INotificationService
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationServiceImpl(
    private val notificationRepository: INotificationRepository
) : INotificationService {

    @Transactional(readOnly = true)
    override fun getNotifications(userId: UUID): List<Notification> {
        return notificationRepository.findByUserId(userId)
    }

    @Transactional(readOnly = true)
    override fun getUnreadNotifications(userId: UUID): List<Notification> {
        return notificationRepository.findUnreadByUserId(userId)
    }

    @Transactional
    override fun markAsRead(userId: UUID, notificationId: UUID) {
        notificationRepository.markAsRead(notificationId, userId)
    }

    @Transactional
    override fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsRead(userId)
    }
}
