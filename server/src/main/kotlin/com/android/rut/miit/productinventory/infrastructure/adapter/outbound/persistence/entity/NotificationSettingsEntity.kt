package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_settings")
class NotificationSettingsEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "expiration_reminders_enabled", nullable = false)
    var expirationRemindersEnabled: Boolean = true,

    @Column(name = "low_stock_reminders_enabled", nullable = false)
    var lowStockRemindersEnabled: Boolean = true,

    @Column(name = "push_enabled", nullable = false)
    var pushEnabled: Boolean = true,

    @Column(name = "expiration_reminder_days", nullable = false)
    var expirationReminderDays: Int = 3,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
