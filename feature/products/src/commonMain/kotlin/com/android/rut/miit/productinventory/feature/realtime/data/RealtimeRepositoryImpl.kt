package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.feature.realtime.api.RealtimeRepository
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import com.android.rut.miit.productinventory.feature.realtime.data.mappers.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealtimeRepositoryImpl(
    private val eventSource: RealtimeEventSource
) : RealtimeRepository {
    override fun observeHouseholdEvents(householdId: String): Flow<HouseholdRealtimeEvent> =
        eventSource.observeHouseholdEvents(householdId).map { it.toDomain() }
}
