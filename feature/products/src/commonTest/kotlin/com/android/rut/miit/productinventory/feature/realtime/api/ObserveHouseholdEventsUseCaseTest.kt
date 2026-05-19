package com.android.rut.miit.productinventory.feature.realtime.api

import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class ObserveHouseholdEventsUseCaseTest {

    @Test
    fun `delegates household event observation to repository`() = runTest {
        val expected = HouseholdRealtimeEvent.ResyncRequired(
            householdId = "household-id",
            occurredAt = "2026-05-14T00:00:00Z",
            reason = "test"
        )
        val repository = object : RealtimeRepository {
            override fun observeHouseholdEvents(householdId: String) = flowOf(expected)
        }

        assertEquals(listOf(expected), ObserveHouseholdEventsUseCase(repository)("household-id").toList())
    }
}
