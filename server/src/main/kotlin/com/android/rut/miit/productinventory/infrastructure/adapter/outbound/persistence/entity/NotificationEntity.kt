package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String = "",

    @Column(name = "sent_at", nullable = false)
    var sentAt: Instant = Instant.now(),

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
)
