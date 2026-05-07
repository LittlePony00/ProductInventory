package com.android.rut.miit.productinventory.feature.auth.api

import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, name: String): AuthToken {
        return repository.register(email, password, name)
    }
}
