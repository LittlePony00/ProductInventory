package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.port.inbound.INotificationPreferencesService
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationDeviceTokenRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificationPreferencesServiceImpl(
    private val settingsRepository: INotificationSettingsRepository,
    private val deviceTokenRepository: INotificationDeviceTokenRepository
) : INotificationPreferencesService {

    @Transactional(readOnly = true)
    override fun getSettings(userId: UUID): NotificationSettings =
        settingsRepository.findByUserId(userId) ?: NotificationSettings(userId = userId)

    @Transactional
    override fun updateSettings(
        userId: UUID,
        expirationRemindersEnabled: Boolean?,
        lowStockRemindersEnabled: Boolean?,
        pushEnabled: Boolean?,
        expirationReminderDays: Int?
    ): NotificationSettings {
        val existing = getSettings(userId)
        return settingsRepository.save(
            existing.copy(
                expirationRemindersEnabled = expirationRemindersEnabled ?: existing.expirationRemindersEnabled,
                lowStockRemindersEnabled = lowStockRemindersEnabled ?: existing.lowStockRemindersEnabled,
                pushEnabled = pushEnabled ?: existing.pushEnabled,
                expirationReminderDays = expirationReminderDays ?: existing.expirationReminderDays,
                updatedAt = Instant.now()
            )
        )
    }

    @Transactional
    override fun registerDeviceToken(
        userId: UUID,
        token: String,
        platform: NotificationPlatform
    ): NotificationDeviceToken {
        val normalizedToken = token.trim()
        require(normalizedToken.isNotBlank()) { "Device token is required" }
        return deviceTokenRepository.upsert(userId, normalizedToken, platform)
    }

    @Transactional
    override fun deactivateDeviceToken(userId: UUID, tokenId: UUID) {
        deviceTokenRepository.deactivate(userId, tokenId)
    }
}
