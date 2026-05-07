package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    var email: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
