package com.android.rut.miit.productinventory.feature.realtime.api.models

import com.android.rut.miit.productinventory.feature.products.api.models.Product

sealed class HouseholdRealtimeEvent {
    abstract val householdId: String
    abstract val occurredAt: String

    data class ProductCreated(
        override val householdId: String,
        override val occurredAt: String,
        val product: Product
    ) : HouseholdRealtimeEvent()

    data class ProductUpdated(
        override val householdId: String,
        override val occurredAt: String,
        val product: Product
    ) : HouseholdRealtimeEvent()

    data class ProductDeleted(
        override val householdId: String,
        override val occurredAt: String,
        val productId: String
    ) : HouseholdRealtimeEvent()

    data class ResyncRequired(
        override val householdId: String,
        override val occurredAt: String,
        val reason: String? = null
    ) : HouseholdRealtimeEvent()
}
