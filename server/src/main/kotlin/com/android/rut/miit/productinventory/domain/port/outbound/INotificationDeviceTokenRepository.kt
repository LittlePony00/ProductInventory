package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import java.util.UUID

interface INotificationDeviceTokenRepository {
    fun findActiveByUserId(userId: UUID): List<NotificationDeviceToken>
    fun upsert(userId: UUID, token: String, platform: NotificationPlatform): NotificationDeviceToken
    fun deactivate(userId: UUID, tokenId: UUID)
    fun deactivateByToken(token: String)
}
