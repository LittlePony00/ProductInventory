package com.android.rut.miit.productinventory.application.dto.request

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    val name: String? = null
)
