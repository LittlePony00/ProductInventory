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
    brand = brand,
    barcode = barcode,
    category = category,
    categoryId = categoryId,
    categoryName = categoryName,
    quantity = quantity.value,
    quantityUnit = quantity.unit,
    packageAmount = packageQuantity?.value,
    packageUnit = packageQuantity?.unit,
    ingredientsText = ingredientsText,
    calories = calories,
    protein = protein,
    fat = fat,
    carbs = carbs,
    purchaseDate = purchaseDate,
    remainingAmount = remainingAmount,
    lowStockThreshold = lowStockThreshold,
    expirationDate = expirationDate?.date,
    expirationStatus = expirationDate?.status ?: ExpirationStatus.UNKNOWN,
    householdId = householdId,
    addedByUserId = addedByUserId,
    createdAt = createdAt
)

fun Category.toResponse() = CategoryResponse(
    id = id,
    householdId = householdId,
    code = code,
    name = name,
    system = system,
    archived = archived,
    createdAt = createdAt
)

fun Household.toResponse() = HouseholdResponse(
    id = id,
    name = name,
    createdAt = createdAt
)

fun Notification.toResponse() = NotificationResponse(
    id = id,
    type = type,
    title = title,
    message = message,
    householdId = householdId,
    productId = productId,
    sentAt = sentAt,
    isRead = isRead
)

fun Recipe.toResponse() = RecipeResponse(
    title = title,
    ingredients = ingredients.map {
        RecipeIngredientResponse(name = it.name, amount = it.amount)
    },
    steps = steps,
    time = time,
    calories = calories
)

fun Membership.toResponse(user: User) = MembershipResponse(
    userId = userId,
    userName = user.name,
    userEmail = user.email,
    role = role,
    joinedAt = joinedAt
)
