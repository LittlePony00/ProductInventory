package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.User
import com.android.rut.miit.productinventory.domain.port.inbound.IUserService
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserServiceImpl(
    private val userRepository: IUserRepository
) : IUserService {

    @Transactional(readOnly = true)
    override fun getProfile(userId: UUID): User {
        return userRepository.findById(userId)
            ?: throw EntityNotFoundException("User", userId)
    }

    @Transactional
    override fun updateProfile(userId: UUID, name: String?): User {
        val user = userRepository.findById(userId)
            ?: throw EntityNotFoundException("User", userId)

        val updated = user.copy(
            name = name?.trim() ?: user.name
        )

        return userRepository.save(updated)
    }
}
