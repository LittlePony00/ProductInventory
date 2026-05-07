package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaNotificationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationRepositoryAdapter(
    private val jpaRepository: JpaNotificationRepository
) : INotificationRepository {

    override fun findByUserId(userId: UUID): List<Notification> =
        jpaRepository.findByUserIdOrderBySentAtDesc(userId).map { it.toDomain() }

    override fun findUnreadByUserId(userId: UUID): List<Notification> =
        jpaRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId).map { it.toDomain() }

    override fun save(notification: Notification): Notification =
        jpaRepository.save(notification.toEntity()).toDomain()

    override fun markAsRead(id: UUID, userId: UUID) =
        jpaRepository.markAsRead(id, userId)

    override fun markAllAsRead(userId: UUID) =
        jpaRepository.markAllAsRead(userId)
}
