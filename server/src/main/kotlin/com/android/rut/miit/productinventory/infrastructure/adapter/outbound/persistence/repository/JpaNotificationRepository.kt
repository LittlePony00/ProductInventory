package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.NotificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JpaNotificationRepository : JpaRepository<NotificationEntity, UUID> {
    fun findByUserIdOrderBySentAtDesc(userId: UUID): List<NotificationEntity>
    fun findByUserIdAndIsReadFalseOrderBySentAtDesc(userId: UUID): List<NotificationEntity>

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    fun markAsRead(id: UUID, userId: UUID)

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.userId = :userId")
    fun markAllAsRead(userId: UUID)
}
