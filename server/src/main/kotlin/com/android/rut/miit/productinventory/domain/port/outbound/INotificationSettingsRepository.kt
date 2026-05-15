package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import java.util.UUID

interface INotificationSettingsRepository {
    fun findByUserId(userId: UUID): NotificationSettings?
    fun save(settings: NotificationSettings): NotificationSettings
}
