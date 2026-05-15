package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class NotificationDeviceToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val token: String,
    val platform: NotificationPlatform,
    val active: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val lastSeenAt: Instant = Instant.now()
)

enum class NotificationPlatform {
    ANDROID,
    IOS
}
