package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.scheduler

import com.android.rut.miit.productinventory.domain.port.inbound.IReminderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReminderScheduler(
    private val reminderService: IReminderService
) {
    private val log = LoggerFactory.getLogger(ReminderScheduler::class.java)

    @Scheduled(cron = "\${product-inventory.reminders.cron:0 0 * * * *}")
    fun runDueReminders() {
        val result = reminderService.runDueReminders()
        log.info(
            "Reminder scan finished: expiringProducts={}, lowStockProducts={}, notificationsCreated={}",
            result.expiringProducts,
            result.lowStockProducts,
            result.notificationsCreated
        )
    }
}
