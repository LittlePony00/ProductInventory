package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.NotificationSettingsEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaNotificationSettingsRepository : JpaRepository<NotificationSettingsEntity, UUID>
