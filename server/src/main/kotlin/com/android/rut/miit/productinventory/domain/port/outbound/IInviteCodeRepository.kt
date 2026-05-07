package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.InviteCode
import java.util.UUID

interface IInviteCodeRepository {
    fun findByCode(code: String): InviteCode?
    fun findActiveByHouseholdId(householdId: UUID): List<InviteCode>
    fun save(inviteCode: InviteCode): InviteCode
}
