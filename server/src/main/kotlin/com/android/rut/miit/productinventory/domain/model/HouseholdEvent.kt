package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class HouseholdEvent(
    val id: UUID = UUID.randomUUID(),
    val type: HouseholdEventType,
    val householdId: UUID,
    val actorUserId: UUID,
    val productId: UUID? = null,
    val productName: String? = null,
    val memberUserId: UUID? = null,
    val previousCategory: ProductCategory? = null,
    val currentCategory: ProductCategory? = null,
    val occurredAt: Instant = Instant.now()
)

enum class HouseholdEventType {
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_QUANTITY_CHANGED,
    PRODUCT_DEPLETED,
    CATEGORY_CHANGED,
    MEMBER_JOINED,
    INVENTORY_LOW,
    EXPIRING_SOON
}
