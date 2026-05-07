package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "memberships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "household_id"])]
)
class MembershipEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "household_id", nullable = false)
    var householdId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var role: String = "MEMBER",

    @Column(name = "joined_at", nullable = false)
    var joinedAt: Instant = Instant.now()
)
