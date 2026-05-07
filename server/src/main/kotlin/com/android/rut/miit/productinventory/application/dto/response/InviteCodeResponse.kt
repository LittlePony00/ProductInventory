package com.android.rut.miit.productinventory.application.dto.response

import java.time.Instant

data class InviteCodeResponse(
    val code: String,
    val expiresAt: Instant
)
