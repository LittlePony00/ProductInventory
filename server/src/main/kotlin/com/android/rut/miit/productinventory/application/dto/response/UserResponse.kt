package com.android.rut.miit.productinventory.application.dto.response

import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val name: String
)
