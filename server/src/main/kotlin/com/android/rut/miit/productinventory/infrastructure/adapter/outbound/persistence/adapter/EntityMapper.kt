package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.*

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    name = name,
    passwordHash = passwordHash,
    createdAt = createdAt
)

fun User.toEntity() = UserEntity(
    id = id,
    email = email,
    name = name,
    passwordHash = passwordHash,
    createdAt = createdAt
)

fun HouseholdEntity.toDomain() = Household(
    id = id,
    name = name,
    createdAt = createdAt
)

fun Household.toEntity() = HouseholdEntity(
    id = id,
    name = name,
    createdAt = createdAt
)

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    category = ProductCategory.valueOf(category),
    quantity = Quantity(value = quantity, unit = QuantityUnit.valueOf(quantityUnit)),
    packageQuantity = packageAmount?.let {
        Quantity(value = it, unit = QuantityUnit.valueOf(packageUnit ?: quantityUnit))
    },
    ingredientsText = ingredientsText,
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
    purchaseDate = purchaseDate,
    remainingAmount = remainingAmount ?: quantity,
    lowStockThreshold = lowStockThreshold,
    expirationDate = expirationDate?.let { ExpirationDate(it) },
    householdId = householdId,
    addedByUserId = addedByUserId,
    createdAt = createdAt
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    category = category.name,
    quantity = quantity.value,
    quantityUnit = quantity.unit.name,
    packageAmount = packageQuantity?.value,
    packageUnit = packageQuantity?.unit?.name,
    ingredientsText = ingredientsText,
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
    purchaseDate = purchaseDate,
    remainingAmount = remainingAmount,
    lowStockThreshold = lowStockThreshold,
    expirationDate = expirationDate?.date,
    householdId = householdId,
    addedByUserId = addedByUserId,
    createdAt = createdAt
)

fun MembershipEntity.toDomain() = Membership(
    id = id,
    userId = userId,
    householdId = householdId,
    role = MembershipRole.valueOf(role),
    joinedAt = joinedAt
)

fun Membership.toEntity() = MembershipEntity(
    id = id,
    userId = userId,
    householdId = householdId,
    role = role.name,
    joinedAt = joinedAt
)

fun NotificationEntity.toDomain() = Notification(
    id = id,
    userId = userId,
    title = title,
    message = message,
    sentAt = sentAt,
    isRead = isRead
)

fun Notification.toEntity() = NotificationEntity(
    id = id,
    userId = userId,
    title = title,
    message = message,
    sentAt = sentAt,
    isRead = isRead
)

fun InviteCodeEntity.toDomain() = InviteCode(
    id = id,
    code = code,
    householdId = householdId,
    createdByUserId = createdByUserId,
    createdAt = createdAt,
    expiresAt = expiresAt,
    used = used
)

fun InviteCode.toEntity() = InviteCodeEntity(
    id = id,
    code = code,
    householdId = householdId,
    createdByUserId = createdByUserId,
    createdAt = createdAt,
    expiresAt = expiresAt,
    used = used
)

fun RefreshTokenEntity.toDomain() = RefreshToken(
    id = id,
    token = token,
    userId = userId,
    expiresAt = expiresAt,
    revoked = revoked
)

fun RefreshToken.toEntity() = RefreshTokenEntity(
    id = id,
    token = token,
    userId = userId,
    expiresAt = expiresAt,
    revoked = revoked
)
