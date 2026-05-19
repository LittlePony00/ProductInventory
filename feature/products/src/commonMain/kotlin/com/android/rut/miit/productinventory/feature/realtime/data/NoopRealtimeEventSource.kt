package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NoopRealtimeEventSource : RealtimeEventSource {
    override fun observeHouseholdEvents(householdId: String): Flow<RealtimeEventDto> =
        emptyFlow()
}
