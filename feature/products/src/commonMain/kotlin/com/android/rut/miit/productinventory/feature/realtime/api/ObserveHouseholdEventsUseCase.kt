package com.android.rut.miit.productinventory.feature.realtime.api

import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlinx.coroutines.flow.Flow

class ObserveHouseholdEventsUseCase(
    private val repository: RealtimeRepository
) {
    operator fun invoke(householdId: String): Flow<HouseholdRealtimeEvent> =
        repository.observeHouseholdEvents(householdId)
}
