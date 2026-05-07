package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID = UUID.randomUUID(),
    val token: String,
    val userId: UUID,
    val expiresAt: Instant,
    val revoked: Boolean = false
)
