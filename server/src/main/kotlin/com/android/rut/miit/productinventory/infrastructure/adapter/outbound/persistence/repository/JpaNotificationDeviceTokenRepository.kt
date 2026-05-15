package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.NotificationDeviceTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaNotificationDeviceTokenRepository : JpaRepository<NotificationDeviceTokenEntity, UUID> {
    fun findByUserIdAndActiveTrue(userId: UUID): List<NotificationDeviceTokenEntity>
    fun findByToken(token: String): NotificationDeviceTokenEntity?
}
