package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.*
import com.android.rut.miit.productinventory.domain.model.RefreshToken
import com.android.rut.miit.productinventory.domain.model.User
import com.android.rut.miit.productinventory.domain.port.inbound.AuthResult
import com.android.rut.miit.productinventory.domain.port.inbound.IAuthService
import com.android.rut.miit.productinventory.domain.port.outbound.IRefreshTokenRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import com.android.rut.miit.productinventory.infrastructure.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class AuthServiceImpl(
    private val userRepository: IUserRepository,
    private val refreshTokenRepository: IRefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) : IAuthService {

    @Transactional
    override fun register(email: String, password: String, name: String): AuthResult {
        if (userRepository.existsByEmail(email)) {
            throw DuplicateEntityException("User with email '$email' already exists")
        }

        val user = userRepository.save(
            User(
                email = email.lowercase().trim(),
                name = name.trim(),
                passwordHash = passwordEncoder.encode(password)
            )
        )

        return generateTokens(user)
    }

    @Transactional
    override fun login(email: String, password: String): AuthResult {
        val user = userRepository.findByEmail(email.lowercase().trim())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return generateTokens(user)
    }

    @Transactional
    override fun refreshToken(refreshToken: String): AuthResult {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw InvalidTokenException("Refresh token not found")

        if (storedToken.revoked) {
            throw InvalidTokenException("Refresh token has been revoked")
        }

        if (storedToken.expiresAt.isBefore(Instant.now())) {
            throw TokenExpiredException("Refresh token has expired")
        }

        val user = userRepository.findById(storedToken.userId)
            ?: throw EntityNotFoundException("User", storedToken.userId)

        refreshTokenRepository.revokeAllByUserId(user.id)

        return generateTokens(user)
    }

    private fun generateTokens(user: User): AuthResult {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val refreshTokenValue = UUID.randomUUID().toString()

        refreshTokenRepository.save(
            RefreshToken(
                token = refreshTokenValue,
                userId = user.id,
                expiresAt = Instant.now().plus(30, ChronoUnit.DAYS)
            )
        )

        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshTokenValue,
            userId = user.id
        )
    }
}
