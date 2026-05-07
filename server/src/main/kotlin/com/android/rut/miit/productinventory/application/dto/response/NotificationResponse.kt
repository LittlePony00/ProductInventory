package com.android.rut.miit.productinventory.application.dto.response

import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val title: String,
    val message: String,
    val sentAt: Instant,
    val isRead: Boolean
)
