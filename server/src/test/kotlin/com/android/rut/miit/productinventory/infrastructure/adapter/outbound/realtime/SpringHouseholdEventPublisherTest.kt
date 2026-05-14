package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.context.ApplicationEventPublisher

class SpringHouseholdEventPublisherTest {

    @Test
    fun `publish forwards household event into spring application event publisher`() {
        val applicationEventPublisher = RecordingApplicationEventPublisher()
        val publisher = SpringHouseholdEventPublisher(applicationEventPublisher)
        val event = HouseholdEvent(
            type = HouseholdEventType.PRODUCT_CREATED,
            householdId = UUID.randomUUID(),
            actorUserId = UUID.randomUUID()
        )

        publisher.publish(event)

        assertEquals(1, applicationEventPublisher.events.size)
        assertEquals(event, applicationEventPublisher.events.single())
    }

    private class RecordingApplicationEventPublisher : ApplicationEventPublisher {
        val events = mutableListOf<Any>()

        override fun publishEvent(event: Any) {
            events += event
        }
    }
}
