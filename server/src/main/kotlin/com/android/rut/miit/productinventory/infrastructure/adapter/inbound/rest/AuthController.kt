package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.LoginRequest
import com.android.rut.miit.productinventory.application.dto.request.RefreshTokenRequest
import com.android.rut.miit.productinventory.application.dto.request.RegisterRequest
import com.android.rut.miit.productinventory.application.dto.response.AuthResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: IAuthService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request.email, request.password, request.name).toResponse()
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request.email, request.password).toResponse()
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse {
        return authService.refreshToken(request.refreshToken).toResponse()
    }
}
