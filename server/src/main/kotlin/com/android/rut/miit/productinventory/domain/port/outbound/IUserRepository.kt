package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.User
import java.util.UUID

interface IUserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun existsByEmail(email: String): Boolean
}
