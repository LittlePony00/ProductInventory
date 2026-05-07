package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.UpdateProfileRequest
import com.android.rut.miit.productinventory.application.dto.response.UserResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IUserService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController(
    private val userService: IUserService
) {

    @GetMapping
    fun getProfile(): UserResponse {
        return userService.getProfile(currentUserId()).toResponse()
    }

    @PutMapping
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): UserResponse {
        return userService.updateProfile(currentUserId(), request.name).toResponse()
    }
}
