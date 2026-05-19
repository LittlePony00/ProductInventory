package com.android.rut.miit.productinventory.feature.realtime.api

import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlinx.coroutines.flow.Flow

interface RealtimeRepository {
    fun observeHouseholdEvents(householdId: String): Flow<HouseholdRealtimeEvent>
}
