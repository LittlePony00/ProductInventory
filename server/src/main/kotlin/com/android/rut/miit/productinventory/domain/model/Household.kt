package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Household(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val createdAt: Instant = Instant.now()
)
