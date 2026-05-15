package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSettingsRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaNotificationSettingsRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationSettingsRepositoryAdapter(
    private val jpaRepository: JpaNotificationSettingsRepository
) : INotificationSettingsRepository {
    override fun findByUserId(userId: UUID): NotificationSettings? =
        jpaRepository.findById(userId).orElse(null)?.toDomain()

    override fun save(settings: NotificationSettings): NotificationSettings =
        jpaRepository.save(settings.toEntity()).toDomain()
}
