package com.android.rut.miit.productinventory.application.mapper

import com.android.rut.miit.productinventory.application.dto.response.*
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.AuthResult

fun AuthResult.toResponse() = AuthResponse(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId
)

fun User.toResponse() = UserResponse(
    id = id,
    email = email,
    name = name
)

fun Product.toResponse() = ProductResponse(
    id = id,
    name = name,
    category = category,
    quantity = quantity.value,
    quantityUnit = quantity.unit,
    expirationDate = expirationDate?.date,
    expirationStatus = expirationDate?.status ?: ExpirationStatus.UNKNOWN,
    householdId = householdId,
    addedByUserId = addedByUserId,
    createdAt = createdAt,
    barcode = barcode
)

fun Household.toResponse() = HouseholdResponse(
    id = id,
    name = name,
    createdAt = createdAt
)

fun Notification.toResponse() = NotificationResponse(
    id = id,
    title = title,
    message = message,
    sentAt = sentAt,
    isRead = isRead
)

fun Recipe.toResponse() = RecipeResponse(
    id = id,
    title = title,
    description = description,
    ingredients = ingredients,
    instructions = instructions,
    imageUrl = imageUrl
)

fun Membership.toResponse(user: User) = MembershipResponse(
    userId = userId,
    userName = user.name,
    userEmail = user.email,
    role = role,
    joinedAt = joinedAt
)
