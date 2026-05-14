package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringHouseholdEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher
) : IHouseholdEventPublisher {
    override fun publish(event: HouseholdEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
