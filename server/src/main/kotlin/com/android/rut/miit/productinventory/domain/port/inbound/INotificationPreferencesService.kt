package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import java.util.UUID

interface INotificationPreferencesService {
    fun getSettings(userId: UUID): NotificationSettings
    fun updateSettings(
        userId: UUID,
        expirationRemindersEnabled: Boolean?,
        lowStockRemindersEnabled: Boolean?,
        pushEnabled: Boolean?,
        expirationReminderDays: Int?
    ): NotificationSettings
    fun registerDeviceToken(userId: UUID, token: String, platform: NotificationPlatform): NotificationDeviceToken
    fun deactivateDeviceToken(userId: UUID, tokenId: UUID)
}
