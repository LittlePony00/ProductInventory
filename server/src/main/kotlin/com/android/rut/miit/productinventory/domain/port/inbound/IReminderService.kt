package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.ReminderRunResult
import java.time.LocalDate
import java.util.UUID

interface IReminderService {
    fun runDueReminders(referenceDate: LocalDate = LocalDate.now()): ReminderRunResult
    fun runHouseholdDueReminders(
        userId: UUID,
        householdId: UUID,
        referenceDate: LocalDate = LocalDate.now()
    ): ReminderRunResult
}
