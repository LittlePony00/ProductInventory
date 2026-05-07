package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.RefreshToken
import java.util.UUID

interface IRefreshTokenRepository {
    fun findByToken(token: String): RefreshToken?
    fun save(refreshToken: RefreshToken): RefreshToken
    fun revokeAllByUserId(userId: UUID)
}
