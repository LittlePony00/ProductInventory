package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var token: String = "",

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(nullable = false)
    var revoked: Boolean = false
)
