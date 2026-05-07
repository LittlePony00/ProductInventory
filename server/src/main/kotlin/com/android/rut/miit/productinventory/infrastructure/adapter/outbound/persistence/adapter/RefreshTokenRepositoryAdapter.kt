package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.RefreshToken
import com.android.rut.miit.productinventory.domain.port.outbound.IRefreshTokenRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaRefreshTokenRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RefreshTokenRepositoryAdapter(
    private val jpaRepository: JpaRefreshTokenRepository
) : IRefreshTokenRepository {

    override fun findByToken(token: String): RefreshToken? =
        jpaRepository.findByToken(token)?.toDomain()

    override fun save(refreshToken: RefreshToken): RefreshToken =
        jpaRepository.save(refreshToken.toEntity()).toDomain()

    override fun revokeAllByUserId(userId: UUID) =
        jpaRepository.revokeAllByUserId(userId)
}
