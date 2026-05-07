package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Membership(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val householdId: UUID,
    val role: MembershipRole,
    val joinedAt: Instant = Instant.now()
)
