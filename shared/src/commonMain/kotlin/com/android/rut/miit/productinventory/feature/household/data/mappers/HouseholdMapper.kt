package com.android.rut.miit.productinventory.feature.household.data.mappers

import com.android.rut.miit.productinventory.feature.household.api.models.*
import com.android.rut.miit.productinventory.feature.household.data.models.HouseholdResponseDto
import com.android.rut.miit.productinventory.feature.household.data.models.InviteCodeResponseDto
import com.android.rut.miit.productinventory.feature.household.data.models.MemberResponseDto

fun HouseholdResponseDto.toDomain() = Household(
    id = id,
    name = name,
    createdAt = createdAt
)

fun MemberResponseDto.toDomain() = Member(
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    role = try { MemberRole.valueOf(role) } catch (_: Exception) { MemberRole.MEMBER },
    joinedAt = joinedAt
)

fun InviteCodeResponseDto.toDomain() = InviteCode(
    code = code,
    expiresAt = expiresAt
)
