package com.android.rut.miit.productinventory.feature.household.api.models

data class Household(
    val id: String,
    val name: String,
    val createdAt: String
)

data class Member(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val role: MemberRole,
    val joinedAt: String
)

enum class MemberRole {
    OWNER, MEMBER
}

data class InviteCode(
    val code: String,
    val expiresAt: String
)
