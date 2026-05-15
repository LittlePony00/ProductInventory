package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationDeviceTokenRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaNotificationDeviceTokenRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class NotificationDeviceTokenRepositoryAdapter(
    private val jpaRepository: JpaNotificationDeviceTokenRepository
) : INotificationDeviceTokenRepository {
    override fun findActiveByUserId(userId: UUID): List<NotificationDeviceToken> =
        jpaRepository.findByUserIdAndActiveTrue(userId).map { it.toDomain() }

    override fun upsert(userId: UUID, token: String, platform: NotificationPlatform): NotificationDeviceToken {
        val existing = jpaRepository.findByToken(token)
        val entity = existing ?: NotificationDeviceToken(userId = userId, token = token, platform = platform).toEntity()
        entity.userId = userId
        entity.platform = platform.name
        entity.active = true
        entity.lastSeenAt = Instant.now()
        return jpaRepository.save(entity).toDomain()
    }

    override fun deactivate(userId: UUID, tokenId: UUID) {
        val entity = jpaRepository.findById(tokenId).orElse(null) ?: return
        if (entity.userId != userId) return
        entity.active = false
        jpaRepository.save(entity)
    }

    override fun deactivateByToken(token: String) {
        val entity = jpaRepository.findByToken(token) ?: return
        entity.active = false
        jpaRepository.save(entity)
    }
}
