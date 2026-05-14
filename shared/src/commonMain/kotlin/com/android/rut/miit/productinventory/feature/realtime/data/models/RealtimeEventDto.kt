package com.android.rut.miit.productinventory.feature.realtime.data.models

import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import kotlinx.serialization.Serializable

@Serializable
data class RealtimeEventDto(
    val type: String,
    val householdId: String,
    val occurredAt: String,
    val product: ProductResponseDto? = null,
    val productId: String? = null,
    val reason: String? = null
)
