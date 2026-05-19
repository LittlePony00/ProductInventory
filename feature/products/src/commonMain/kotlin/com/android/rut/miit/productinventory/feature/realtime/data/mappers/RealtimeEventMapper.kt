package com.android.rut.miit.productinventory.feature.realtime.data.mappers

import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto

fun RealtimeEventDto.toDomain(): HouseholdRealtimeEvent =
    when (type) {
        "PRODUCT_CREATED" -> HouseholdRealtimeEvent.ProductCreated(
            householdId = householdId,
            occurredAt = occurredAt,
            product = product?.toDomain()
                ?: return resync("PRODUCT_CREATED event requires inventory resync")
        )
        "PRODUCT_UPDATED" -> HouseholdRealtimeEvent.ProductUpdated(
            householdId = householdId,
            occurredAt = occurredAt,
            product = product?.toDomain()
                ?: return resync("PRODUCT_UPDATED event requires inventory resync")
        )
        "PRODUCT_DELETED" -> HouseholdRealtimeEvent.ProductDeleted(
            householdId = householdId,
            occurredAt = occurredAt,
            productId = requireNotNull(productId) { "PRODUCT_DELETED event requires productId" }
        )
        "PRODUCT_QUANTITY_CHANGED", "PRODUCT_DEPLETED" -> HouseholdRealtimeEvent.ResyncRequired(
            householdId = householdId,
            occurredAt = occurredAt,
            reason = "$type event requires inventory resync"
        )
        "RESYNC_REQUIRED" -> HouseholdRealtimeEvent.ResyncRequired(
            householdId = householdId,
            occurredAt = occurredAt,
            reason = reason
        )
        else -> HouseholdRealtimeEvent.ResyncRequired(
            householdId = householdId,
            occurredAt = occurredAt,
            reason = "Unsupported realtime event type: $type"
        )
    }

private fun RealtimeEventDto.resync(reason: String): HouseholdRealtimeEvent.ResyncRequired =
    HouseholdRealtimeEvent.ResyncRequired(
        householdId = householdId,
        occurredAt = occurredAt,
        reason = reason
    )
