package com.android.rut.miit.productinventory.application.dto.response

import java.time.Instant
import java.util.UUID

data class HouseholdResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant
)
