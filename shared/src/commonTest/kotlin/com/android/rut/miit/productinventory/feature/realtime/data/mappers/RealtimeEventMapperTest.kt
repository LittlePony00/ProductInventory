package com.android.rut.miit.productinventory.feature.realtime.data.mappers

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RealtimeEventMapperTest {

    @Test
    fun `maps product created event`() {
        val event = RealtimeEventDto(
            type = "PRODUCT_CREATED",
            householdId = "household-id",
            occurredAt = "2026-05-14T00:00:00Z",
            product = productDto()
        ).toDomain()

        val created = assertIs<HouseholdRealtimeEvent.ProductCreated>(event)
        assertEquals("household-id", created.householdId)
        assertEquals("product-id", created.product.id)
        assertEquals(ProductCategory.DAIRY, created.product.category)
    }

    @Test
    fun `maps product event without payload to resync required`() {
        val event = RealtimeEventDto(
            type = "PRODUCT_UPDATED",
            householdId = "household-id",
            occurredAt = "2026-05-14T00:00:00Z",
            product = null
        ).toDomain()

        val resync = assertIs<HouseholdRealtimeEvent.ResyncRequired>(event)
        assertEquals("PRODUCT_UPDATED event requires inventory resync", resync.reason)
    }

    @Test
    fun `maps product deleted event`() {
        val event = RealtimeEventDto(
            type = "PRODUCT_DELETED",
            householdId = "household-id",
            occurredAt = "2026-05-14T00:00:00Z",
            productId = "product-id"
        ).toDomain()

        val deleted = assertIs<HouseholdRealtimeEvent.ProductDeleted>(event)
        assertEquals("product-id", deleted.productId)
    }

    @Test
    fun `maps unknown event to resync required`() {
        val event = RealtimeEventDto(
            type = "UNSUPPORTED",
            householdId = "household-id",
            occurredAt = "2026-05-14T00:00:00Z"
        ).toDomain()

        val resync = assertIs<HouseholdRealtimeEvent.ResyncRequired>(event)
        assertEquals("Unsupported realtime event type: UNSUPPORTED", resync.reason)
    }

    private fun productDto(): ProductResponseDto =
        ProductResponseDto(
            id = "product-id",
            name = "Milk",
            category = "DAIRY",
            quantity = 1.0,
            quantityUnit = "PIECES",
            expirationDate = null,
            expirationStatus = "FRESH",
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        )
}
