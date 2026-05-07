package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.User
import java.util.UUID

interface IUserService {
    fun getProfile(userId: UUID): User
    fun updateProfile(userId: UUID, name: String?): User
}
