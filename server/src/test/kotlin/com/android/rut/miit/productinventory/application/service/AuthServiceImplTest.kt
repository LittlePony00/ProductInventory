package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.InvalidCredentialsException
import com.android.rut.miit.productinventory.domain.exception.InvalidTokenException
import com.android.rut.miit.productinventory.domain.exception.TokenExpiredException
import com.android.rut.miit.productinventory.domain.model.RefreshToken
import com.android.rut.miit.productinventory.domain.model.User
import com.android.rut.miit.productinventory.domain.port.outbound.IRefreshTokenRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import com.android.rut.miit.productinventory.infrastructure.security.JwtTokenProvider
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceImplTest {

    @Test
    fun `register stores normalized user and returns valid jwt plus refresh token`() {
        val users = InMemoryUserRepository()
        val refreshTokens = InMemoryRefreshTokenRepository()
        val jwt = JwtTokenProvider(secret = SECRET, accessTokenExpirationMs = 60_000)
        val service = AuthServiceImpl(users, refreshTokens, PrefixPasswordEncoder, jwt)

        val result = service.register(" USER@Example.COM ", "secret", " User ")

        val saved = users.users.single()
        assertEquals("user@example.com", saved.email)
        assertEquals("User", saved.name)
        assertEquals("encoded:secret", saved.passwordHash)
        assertEquals(saved.id, result.userId)
        assertTrue(jwt.validateToken(result.accessToken))
        assertEquals(saved.id, jwt.getUserIdFromToken(result.accessToken))
        assertEquals(saved.id, refreshTokens.findByToken(result.refreshToken)?.userId)
    }

    @Test
    fun `login rejects invalid password`() {
        val user = User(email = "user@example.com", name = "User", passwordHash = "encoded:correct")
        val service = service(users = InMemoryUserRepository(listOf(user)))

        assertFailsWith<InvalidCredentialsException> {
            service.login("user@example.com", "wrong")
        }
    }

    @Test
    fun `refresh revokes previous tokens and issues replacement tokens`() {
        val user = User(email = "user@example.com", name = "User", passwordHash = "encoded:secret")
        val oldToken = RefreshToken(
            token = "old-refresh",
            userId = user.id,
            expiresAt = Instant.now().plusSeconds(60)
        )
        val refreshTokens = InMemoryRefreshTokenRepository(listOf(oldToken))
        val jwt = JwtTokenProvider(secret = SECRET, accessTokenExpirationMs = 60_000)
        val service = AuthServiceImpl(
            userRepository = InMemoryUserRepository(listOf(user)),
            refreshTokenRepository = refreshTokens,
            passwordEncoder = PrefixPasswordEncoder,
            jwtTokenProvider = jwt
        )

        val result = service.refreshToken(oldToken.token)

        assertNotEquals(oldToken.token, result.refreshToken)
        assertTrue(refreshTokens.findByToken(oldToken.token)?.revoked == true)
        assertEquals(user.id, refreshTokens.findByToken(result.refreshToken)?.userId)
        assertTrue(jwt.validateToken(result.accessToken))
        assertEquals(user.id, jwt.getUserIdFromToken(result.accessToken))
    }

    @Test
    fun `refresh rejects revoked and expired tokens`() {
        val user = User(email = "user@example.com", name = "User", passwordHash = "encoded:secret")
        val refreshTokens = InMemoryRefreshTokenRepository(
            listOf(
                RefreshToken(
                    token = "revoked-refresh",
                    userId = user.id,
                    expiresAt = Instant.now().plusSeconds(60),
                    revoked = true
                ),
                RefreshToken(
                    token = "expired-refresh",
                    userId = user.id,
                    expiresAt = Instant.now().minusSeconds(60)
                )
            )
        )
        val service = service(
            users = InMemoryUserRepository(listOf(user)),
            refreshTokens = refreshTokens
        )

        assertFailsWith<InvalidTokenException> {
            service.refreshToken("revoked-refresh")
        }
        assertFailsWith<TokenExpiredException> {
            service.refreshToken("expired-refresh")
        }
    }

    private fun service(
        users: InMemoryUserRepository,
        refreshTokens: InMemoryRefreshTokenRepository = InMemoryRefreshTokenRepository()
    ): AuthServiceImpl =
        AuthServiceImpl(
            userRepository = users,
            refreshTokenRepository = refreshTokens,
            passwordEncoder = PrefixPasswordEncoder,
            jwtTokenProvider = JwtTokenProvider(secret = SECRET, accessTokenExpirationMs = 60_000)
        )

    private class InMemoryUserRepository(initialUsers: List<User> = emptyList()) : IUserRepository {
        private val stored = initialUsers.associateBy { it.id }.toMutableMap()
        val users: List<User> get() = stored.values.toList()

        override fun findById(id: UUID): User? = stored[id]
        override fun findByEmail(email: String): User? = stored.values.firstOrNull { it.email == email }
        override fun save(user: User): User {
            stored[user.id] = user
            return user
        }

        override fun existsByEmail(email: String): Boolean =
            stored.values.any { it.email == email }
    }

    private class InMemoryRefreshTokenRepository(
        initialTokens: List<RefreshToken> = emptyList()
    ) : IRefreshTokenRepository {
        private val tokens = initialTokens.associateBy { it.token }.toMutableMap()

        override fun findByToken(token: String): RefreshToken? = tokens[token]
        override fun save(refreshToken: RefreshToken): RefreshToken {
            tokens[refreshToken.token] = refreshToken
            return refreshToken
        }

        override fun revokeAllByUserId(userId: UUID) {
            tokens.replaceAll { _, token ->
                if (token.userId == userId) token.copy(revoked = true) else token
            }
        }
    }

    private object PrefixPasswordEncoder : PasswordEncoder {
        override fun encode(rawPassword: CharSequence): String = "encoded:$rawPassword"
        override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
            encodedPassword == encode(rawPassword)
    }

    private companion object {
        const val SECRET = "01234567890123456789012345678901"
    }
}
