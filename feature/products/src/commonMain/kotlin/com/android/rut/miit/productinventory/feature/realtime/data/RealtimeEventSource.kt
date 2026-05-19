package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import kotlinx.coroutines.flow.Flow

interface RealtimeEventSource {
    fun observeHouseholdEvents(householdId: String): Flow<RealtimeEventDto>
}
