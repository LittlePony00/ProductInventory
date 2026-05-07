package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.MembershipRole
import java.time.Instant
import java.util.UUID

data class MembershipResponse(
    val userId: UUID,
    val userName: String,
    val userEmail: String,
    val role: MembershipRole,
    val joinedAt: Instant
)
