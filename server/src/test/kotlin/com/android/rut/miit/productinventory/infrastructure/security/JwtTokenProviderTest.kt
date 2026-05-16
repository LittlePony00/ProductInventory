package com.android.rut.miit.productinventory.infrastructure.security

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTokenProviderTest {

    @Test
    fun `generated token validates and carries user id`() {
        val userId = UUID.randomUUID()
        val provider = JwtTokenProvider(secret = SECRET, accessTokenExpirationMs = 60_000)

        val token = provider.generateAccessToken(userId, "user@example.com")

        assertTrue(provider.validateToken(token))
        assertEquals(userId, provider.getUserIdFromToken(token))
    }

    @Test
    fun `invalid and expired tokens are rejected`() {
        val provider = JwtTokenProvider(secret = SECRET, accessTokenExpirationMs = -1)
        val expiredToken = provider.generateAccessToken(UUID.randomUUID(), "user@example.com")

        assertFalse(provider.validateToken("not-a-jwt"))
        assertFalse(provider.validateToken(expiredToken))
    }

    private companion object {
        const val SECRET = "01234567890123456789012345678901"
    }
}
