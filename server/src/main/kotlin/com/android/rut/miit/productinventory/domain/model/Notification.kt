package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val title: String,
    val message: String,
    val sentAt: Instant = Instant.now(),
    val isRead: Boolean = false
)
