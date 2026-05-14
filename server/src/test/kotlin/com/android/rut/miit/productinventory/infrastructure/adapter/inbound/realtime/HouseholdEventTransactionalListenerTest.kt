package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class HouseholdEventTransactionalListenerTest {

    @Test
    fun `listener publishes committed household event to broadcaster`() {
        val emitter = RecordingSseEmitter()
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { emitter })
        val listener = HouseholdEventTransactionalListener(broadcaster)
        val event = HouseholdEvent(
            type = HouseholdEventType.MEMBER_JOINED,
            householdId = UUID.randomUUID(),
            actorUserId = UUID.randomUUID(),
            memberUserId = UUID.randomUUID()
        )
        broadcaster.subscribe(event.householdId)

        listener.onHouseholdEvent(event)

        assertTrue(event in emitter.sentData)
    }

    private class RecordingSseEmitter : SseEmitter(0L) {
        val sentData = mutableListOf<Any>()

        override fun send(builder: SseEventBuilder) {
            sentData.addAll(builder.build().mapNotNull { it.data })
        }
    }
}
