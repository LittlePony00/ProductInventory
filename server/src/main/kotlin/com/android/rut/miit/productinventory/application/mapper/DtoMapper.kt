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

fun UserFoodPreferences.toResponse() = FoodPreferencesResponse(
    preferredCuisines = preferredCuisines,
    preferredProducts = preferredProducts,
    dislikedIngredients = dislikedIngredients,
    avoidedProducts = avoidedProducts,
    allergies = allergies,
    dietaryRestrictions = dietaryRestrictions,
    preferredProductIds = preferredProductIds,
    avoidedProductIds = avoidedProductIds,
    preferredCategoryIds = preferredCategoryIds,
    avoidedCategoryIds = avoidedCategoryIds,
    maxCookingTimeMinutes = maxCookingTimeMinutes,
    preferredDifficulty = preferredDifficulty,
    servings = servings
)

fun FoodPreferencesOptions.toResponse() = FoodPreferencesOptionsResponse(
    products = products.map {
        FoodPreferenceProductOptionResponse(
            id = it.id,
            name = it.name,
            categoryId = it.categoryId,
            categoryName = it.categoryName
        )
    },
    categories = categories.map {
        FoodPreferenceCategoryOptionResponse(
            id = it.id,
            name = it.name,
            system = it.system
        )
    }
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
    imageUrl = imageUrl,
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
    calories = calories,
    caloriesKnown = caloriesKnown
)

fun RecipeRecommendation.toResponse() = RecipeResponse(
    id = id,
    title = title,
    ingredients = ingredients.map {
        RecipeIngredientResponse(name = it.name, amount = it.amount)
    },
    steps = steps,
    time = time,
    cookingTimeMinutes = cookingTimeMinutes,
    calories = calories,
    caloriesKnown = caloriesKnown,
    source = source,
    sourceUrl = sourceUrl,
    imageUrl = imageUrl,
    score = score,
    usedHouseholdProducts = usedHouseholdProducts,
    usedExpiringProducts = usedExpiringProducts,
    missingIngredients = missingIngredients,
    reasons = reasons,
    warnings = warnings,
    aiAssisted = aiAssisted,
    aiGenerated = aiGenerated
)

fun RecipeIngredientOption.toResponse() = RecipeIngredientOptionResponse(
    id = id,
    name = name,
    categoryName = categoryName,
    remainingAmount = remainingAmount,
    unit = unit,
    expiring = expiring
)

fun Membership.toResponse(user: User) = MembershipResponse(
    userId = userId,
    userName = user.name,
    userEmail = user.email,
    role = role,
    joinedAt = joinedAt
)
