package com.android.rut.miit.productinventory.feature.household.data.models

import kotlinx.serialization.Serializable

@Serializable
data class HouseholdResponseDto(
    val id: String,
    val name: String,
    val createdAt: String
)

@Serializable
data class MemberResponseDto(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val role: String,
    val joinedAt: String
)

@Serializable
data class InviteCodeResponseDto(
    val code: String,
    val expiresAt: String
)

@Serializable
data class CreateHouseholdRequestDto(
    val name: String
)

@Serializable
data class JoinHouseholdRequestDto(
    val inviteCode: String
)
