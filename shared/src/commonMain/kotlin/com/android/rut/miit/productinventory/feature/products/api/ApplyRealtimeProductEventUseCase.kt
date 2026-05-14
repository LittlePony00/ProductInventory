package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent

class ApplyRealtimeProductEventUseCase(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(event: HouseholdRealtimeEvent) {
        when (event) {
            is HouseholdRealtimeEvent.ProductCreated -> repository.upsertCachedProduct(event.product)
            is HouseholdRealtimeEvent.ProductUpdated -> repository.upsertCachedProduct(event.product)
            is HouseholdRealtimeEvent.ProductDeleted -> repository.deleteCachedProduct(event.productId)
            is HouseholdRealtimeEvent.ResyncRequired -> Unit
        }
    }
}
