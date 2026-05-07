package com.android.rut.miit.productinventory.infrastructure.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @param:Value("\${jwt.secret}") private val secret: String,
    @param:Value("\${jwt.access-token-expiration-ms}") private val accessTokenExpirationMs: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirationMs)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = parseClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
