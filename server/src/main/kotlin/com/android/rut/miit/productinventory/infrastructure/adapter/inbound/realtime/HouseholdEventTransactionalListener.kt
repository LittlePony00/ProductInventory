package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class HouseholdEventTransactionalListener(
    private val broadcaster: HouseholdEventSseBroadcaster
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onHouseholdEvent(event: HouseholdEvent) {
        broadcaster.publish(event)
    }
}
