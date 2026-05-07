package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Household
import java.util.UUID

interface IHouseholdRepository {
    fun findById(id: UUID): Household?
    fun save(household: Household): Household
    fun deleteById(id: UUID)
}
