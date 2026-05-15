package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.NotificationType
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val householdId: UUID?,
    val productId: UUID?,
    val sentAt: Instant,
    val isRead: Boolean
)
