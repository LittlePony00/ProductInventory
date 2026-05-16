package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.User
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class UserServiceImplTest {

    @Test
    fun `profile can be loaded and updated`() {
        val user = User(email = "user@example.com", name = "Old name", passwordHash = "hash")
        val repository = InMemoryUserRepository(listOf(user))
        val service = UserServiceImpl(repository)

        assertEquals(user, service.getProfile(user.id))

        val updated = service.updateProfile(user.id, " New name ")

        assertEquals("New name", updated.name)
        assertEquals("New name", service.getProfile(user.id).name)
    }

    private class InMemoryUserRepository(initialUsers: List<User>) : IUserRepository {
        private val users = initialUsers.associateBy { it.id }.toMutableMap()

        override fun findById(id: UUID): User? = users[id]
        override fun findByEmail(email: String): User? = users.values.firstOrNull { it.email == email }
        override fun save(user: User): User {
            users[user.id] = user
            return user
        }

        override fun existsByEmail(email: String): Boolean =
            users.values.any { it.email == email }
    }
}
