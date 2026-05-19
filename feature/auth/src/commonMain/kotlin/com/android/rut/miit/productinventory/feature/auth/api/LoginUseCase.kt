package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthToken {
        return repository.login(email, password)
    }
}
