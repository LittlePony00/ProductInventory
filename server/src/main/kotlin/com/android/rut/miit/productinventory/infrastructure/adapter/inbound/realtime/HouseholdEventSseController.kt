package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.port.inbound.IHouseholdService
import com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.currentUserId
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/events")
class HouseholdEventSseController(
    private val householdService: IHouseholdService,
    private val broadcaster: HouseholdEventSseBroadcaster
) {
    @GetMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
        @PathVariable householdId: UUID,
        @RequestHeader(name = "Last-Event-ID", required = false) lastEventId: UUID? = null
    ): SseEmitter {
        householdService.getHousehold(currentUserId(), householdId)
        return broadcaster.subscribe(householdId, lastEventId)
    }

    @GetMapping("/missed")
    fun missedEvents(
        @PathVariable householdId: UUID,
        @RequestHeader(name = "Last-Event-ID", required = false) lastEventId: UUID? = null
    ): List<HouseholdEvent> {
        householdService.getHousehold(currentUserId(), householdId)
        return broadcaster.missedEvents(householdId, lastEventId)
    }
}
