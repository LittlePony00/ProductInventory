package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class InviteCode(
    val id: UUID = UUID.randomUUID(),
    val code: String,
    val householdId: UUID,
    val createdByUserId: UUID,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant,
    val used: Boolean = false
)
