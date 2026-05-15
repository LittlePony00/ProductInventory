package com.android.rut.miit.productinventory.application.dto.response

data class ReminderRunResponse(
    val expiringProducts: Int,
    val lowStockProducts: Int,
    val notificationsCreated: Int
)
