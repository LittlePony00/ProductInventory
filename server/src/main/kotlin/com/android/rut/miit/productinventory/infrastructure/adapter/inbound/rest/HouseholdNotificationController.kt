package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.response.ReminderRunResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IReminderService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/notifications")
class HouseholdNotificationController(
    private val reminderService: IReminderService
) {
    @PostMapping("/reminders/run")
    fun runReminderScan(@PathVariable householdId: UUID): ReminderRunResponse {
        val result = reminderService.runHouseholdDueReminders(currentUserId(), householdId)
        return ReminderRunResponse(
            expiringProducts = result.expiringProducts,
            lowStockProducts = result.lowStockProducts,
            notificationsCreated = result.notificationsCreated
        )
    }
}
