package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import com.android.rut.miit.productinventory.feature.products.api.models.Product

class ApplyRealtimeProductEventUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(event: HouseholdRealtimeEvent): Product? =
        when (event) {
            is HouseholdRealtimeEvent.ProductCreated -> repository.upsertCachedProduct(event.product)
            is HouseholdRealtimeEvent.ProductUpdated -> repository.upsertCachedProduct(event.product)
            is HouseholdRealtimeEvent.ProductDeleted -> {
                repository.deleteCachedProduct(event.productId)
                null
            }
            is HouseholdRealtimeEvent.ResyncRequired -> null
        }
}
