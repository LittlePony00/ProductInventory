package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationDeviceTokenRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSettingsRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPreferencesServiceImplTest {

    @Test
    fun `get settings returns defaults when user has no saved settings`() {
        val userId = UUID.randomUUID()
        val service = service()

        val settings = service.getSettings(userId)

        assertEquals(userId, settings.userId)
        assertTrue(settings.expirationRemindersEnabled)
        assertTrue(settings.lowStockRemindersEnabled)
        assertTrue(settings.pushEnabled)
        assertEquals(NotificationSettings.DEFAULT_EXPIRATION_REMINDER_DAYS, settings.expirationReminderDays)
    }

    @Test
    fun `update settings persists explicit values and keeps omitted values`() {
        val userId = UUID.randomUUID()
        val settingsRepository = InMemoryNotificationSettingsRepository()
        val service = service(settingsRepository = settingsRepository)

        val updated = service.updateSettings(
            userId = userId,
            expirationRemindersEnabled = false,
            lowStockRemindersEnabled = null,
            pushEnabled = false,
            expirationReminderDays = 7
        )

        assertFalse(updated.expirationRemindersEnabled)
        assertTrue(updated.lowStockRemindersEnabled)
        assertFalse(updated.pushEnabled)
        assertEquals(7, updated.expirationReminderDays)
        assertEquals(updated, settingsRepository.findByUserId(userId))
    }

    @Test
    fun `register device token trims token and upserts active token`() {
        val userId = UUID.randomUUID()
        val tokenRepository = InMemoryNotificationDeviceTokenRepository()
        val service = service(tokenRepository = tokenRepository)

        val first = service.registerDeviceToken(userId, "  token-1  ", NotificationPlatform.ANDROID)
        val second = service.registerDeviceToken(userId, "token-1", NotificationPlatform.ANDROID)

        assertEquals(first.id, second.id)
        assertEquals("token-1", second.token)
        assertTrue(second.active)
        assertEquals(listOf(second), tokenRepository.findActiveByUserId(userId))
    }

    @Test
    fun `register device token rejects blank token`() {
        val service = service()

        assertFailsWith<IllegalArgumentException> {
            service.registerDeviceToken(UUID.randomUUID(), "  ", NotificationPlatform.ANDROID)
        }
    }

    @Test
    fun `deactivate device token deactivates only requesting user token`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val tokenRepository = InMemoryNotificationDeviceTokenRepository()
        val service = service(tokenRepository = tokenRepository)
        val token = service.registerDeviceToken(userId, "token-1", NotificationPlatform.ANDROID)
        service.registerDeviceToken(otherUserId, "token-2", NotificationPlatform.IOS)

        service.deactivateDeviceToken(userId, token.id)

        assertEquals(emptyList(), tokenRepository.findActiveByUserId(userId))
        assertEquals(1, tokenRepository.findActiveByUserId(otherUserId).size)
    }

    private fun service(
        settingsRepository: InMemoryNotificationSettingsRepository = InMemoryNotificationSettingsRepository(),
        tokenRepository: InMemoryNotificationDeviceTokenRepository = InMemoryNotificationDeviceTokenRepository()
    ): NotificationPreferencesServiceImpl =
        NotificationPreferencesServiceImpl(
            settingsRepository = settingsRepository,
            deviceTokenRepository = tokenRepository
        )

    private class InMemoryNotificationSettingsRepository : INotificationSettingsRepository {
        private val settingsByUser = mutableMapOf<UUID, NotificationSettings>()

        override fun findByUserId(userId: UUID): NotificationSettings? = settingsByUser[userId]

        override fun save(settings: NotificationSettings): NotificationSettings {
            settingsByUser[settings.userId] = settings
            return settings
        }
    }

    private class InMemoryNotificationDeviceTokenRepository : INotificationDeviceTokenRepository {
        private val tokensById = linkedMapOf<UUID, NotificationDeviceToken>()

        override fun findActiveByUserId(userId: UUID): List<NotificationDeviceToken> =
            tokensById.values.filter { it.userId == userId && it.active }

        override fun upsert(userId: UUID, token: String, platform: NotificationPlatform): NotificationDeviceToken {
            val existing = tokensById.values.firstOrNull { it.token == token }
            val saved = existing?.copy(userId = userId, platform = platform, active = true)
                ?: NotificationDeviceToken(userId = userId, token = token, platform = platform)
            tokensById[saved.id] = saved
            return saved
        }

        override fun deactivate(userId: UUID, tokenId: UUID) {
            val existing = tokensById[tokenId]?.takeIf { it.userId == userId } ?: return
            tokensById[tokenId] = existing.copy(active = false)
        }

        override fun deactivateByToken(token: String) {
            val existing = tokensById.values.firstOrNull { it.token == token } ?: return
            tokensById[existing.id] = existing.copy(active = false)
        }
    }
}
