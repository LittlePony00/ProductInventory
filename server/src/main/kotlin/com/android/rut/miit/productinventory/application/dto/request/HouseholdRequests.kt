package com.android.rut.miit.productinventory.application.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateHouseholdRequest(
    @field:NotBlank(message = "Household name is required")
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    val name: String
)

data class JoinHouseholdRequest(
    @field:NotBlank(message = "Invite code is required")
    val inviteCode: String
)
