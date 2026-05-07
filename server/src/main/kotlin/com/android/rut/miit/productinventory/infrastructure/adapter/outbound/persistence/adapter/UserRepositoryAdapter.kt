package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.User
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaUserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserRepositoryAdapter(
    private val jpaRepository: JpaUserRepository
) : IUserRepository {

    override fun findById(id: UUID): User? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? =
        jpaRepository.findByEmail(email)?.toDomain()

    override fun save(user: User): User =
        jpaRepository.save(user.toEntity()).toDomain()

    override fun existsByEmail(email: String): Boolean =
        jpaRepository.existsByEmail(email)
}
