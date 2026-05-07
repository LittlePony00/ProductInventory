package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "invite_codes")
class InviteCodeEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var code: String = "",

    @Column(name = "household_id", nullable = false)
    var householdId: UUID = UUID.randomUUID(),

    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(nullable = false)
    var used: Boolean = false
)
