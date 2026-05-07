package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "households")
class HouseholdEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
