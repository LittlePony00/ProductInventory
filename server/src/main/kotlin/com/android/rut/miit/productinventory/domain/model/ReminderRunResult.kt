package com.android.rut.miit.productinventory.domain.model

data class ReminderRunResult(
    val expiringProducts: Int,
    val lowStockProducts: Int,
    val notificationsCreated: Int
)
