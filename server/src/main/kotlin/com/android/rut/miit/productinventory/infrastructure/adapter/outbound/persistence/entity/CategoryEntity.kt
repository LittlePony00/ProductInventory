package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "categories")
class CategoryEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "household_id")
    var householdId: UUID? = null,

    @Column(name = "code")
    var code: String? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var system: Boolean = false,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
