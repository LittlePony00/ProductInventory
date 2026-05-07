package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JpaRefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    fun revokeAllByUserId(userId: UUID)
}
