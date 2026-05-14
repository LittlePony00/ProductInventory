package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class HouseholdEventSseBroadcasterTest {

    @Test
    fun `publish sends event only to subscribers of same household`() {
        val targetEmitter = RecordingSseEmitter()
        val otherEmitter = RecordingSseEmitter()
        val emitters = ArrayDeque(listOf(targetEmitter, otherEmitter))
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { emitters.removeFirst() })
        val householdId = UUID.randomUUID()
        val otherHouseholdId = UUID.randomUUID()
        broadcaster.subscribe(householdId)
        broadcaster.subscribe(otherHouseholdId)
        val event = HouseholdEvent(
            type = HouseholdEventType.PRODUCT_CREATED,
            householdId = householdId,
            actorUserId = UUID.randomUUID()
        )

        broadcaster.publish(event)

        assertTrue(event in targetEmitter.sentData)
        assertEquals(0, otherEmitter.sentData.size)
    }

    @Test
    fun `subscribe replays missed events after last event id`() {
        val firstEmitter = RecordingSseEmitter()
        val reconnectEmitter = RecordingSseEmitter()
        val emitters = ArrayDeque(listOf(firstEmitter, reconnectEmitter))
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { emitters.removeFirst() })
        val householdId = UUID.randomUUID()
        val firstEvent = event(householdId)
        val secondEvent = event(householdId)
        broadcaster.subscribe(householdId)
        broadcaster.publish(firstEvent)
        broadcaster.publish(secondEvent)

        broadcaster.subscribe(householdId, firstEvent.id)

        assertTrue(secondEvent in reconnectEmitter.sentData)
        assertTrue(firstEvent !in reconnectEmitter.sentData)
    }

    @Test
    fun `missed events endpoint support returns all retained events when last event id is unknown`() {
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { RecordingSseEmitter() })
        val householdId = UUID.randomUUID()
        val firstEvent = event(householdId)
        val secondEvent = event(householdId)
        broadcaster.publish(firstEvent)
        broadcaster.publish(secondEvent)

        assertEquals(listOf(firstEvent, secondEvent), broadcaster.missedEvents(householdId, UUID.randomUUID()))
    }

    @Test
    fun `publish removes emitter after send failure`() {
        val failingEmitter = FailingSseEmitter()
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { failingEmitter })
        val householdId = UUID.randomUUID()
        broadcaster.subscribe(householdId)

        broadcaster.publish(event(householdId))
        broadcaster.publish(event(householdId))

        assertEquals(1, failingEmitter.sendAttempts)
    }

    private class RecordingSseEmitter : SseEmitter(0L) {
        val sentData = mutableListOf<Any>()

        override fun send(builder: SseEventBuilder) {
            val data = builder.build().mapNotNull { data ->
                data.data
            }
            sentData.addAll(data)
        }
    }

    private class FailingSseEmitter : SseEmitter(0L) {
        var sendAttempts = 0

        override fun send(builder: SseEventBuilder) {
            sendAttempts += 1
            throw IOException("dead emitter")
        }
    }

    private companion object {
        fun event(householdId: UUID): HouseholdEvent =
            HouseholdEvent(
                type = HouseholdEventType.PRODUCT_CREATED,
                householdId = householdId,
                actorUserId = UUID.randomUUID()
            )
    }
}
