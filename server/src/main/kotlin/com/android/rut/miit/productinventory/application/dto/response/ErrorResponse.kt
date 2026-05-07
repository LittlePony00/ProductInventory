package com.android.rut.miit.productinventory.application.dto.response

import java.time.Instant

data class ErrorResponse(
    val message: String,
    val code: String,
    val timestamp: Instant = Instant.now()
)
