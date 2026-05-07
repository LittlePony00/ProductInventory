package com.android.rut.miit.productinventory.domain.model

import java.time.LocalDate

data class ExpirationDate(
    val date: LocalDate
) {
    val status: ExpirationStatus
        get() = when {
            date.isBefore(LocalDate.now()) -> ExpirationStatus.EXPIRED
            date.isBefore(LocalDate.now().plusDays(3)) -> ExpirationStatus.EXPIRING_SOON
            else -> ExpirationStatus.FRESH
        }
}
