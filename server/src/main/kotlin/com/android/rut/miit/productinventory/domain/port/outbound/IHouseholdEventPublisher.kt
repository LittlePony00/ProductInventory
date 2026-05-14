package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent

interface IHouseholdEventPublisher {
    fun publish(event: HouseholdEvent)
}
