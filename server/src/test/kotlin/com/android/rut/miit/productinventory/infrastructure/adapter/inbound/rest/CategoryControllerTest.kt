package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.CreateCategoryRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateCategoryRequest
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.port.inbound.ICategoryService
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class CategoryControllerTest {

    @AfterTest
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `get categories forwards household and archived flag`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val category = Category(householdId = householdId, name = "Bakery")
        val service = RecordingCategoryService(listOf(category))
        val controller = CategoryController(service)
        authenticate(userId)

        val response = controller.getCategories(householdId, includeArchived = true)

        val call = service.getCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(householdId, call.householdId)
        assertEquals(true, call.includeArchived)
        assertEquals(category.id, response.single().id)
        assertEquals("Bakery", response.single().name)
    }

    @Test
    fun `create update and archive forward authenticated user`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val service = RecordingCategoryService(emptyList())
        val controller = CategoryController(service)
        authenticate(userId)

        controller.createCategory(householdId, CreateCategoryRequest("Bakery"))
        controller.updateCategory(householdId, categoryId, UpdateCategoryRequest("Bread"))
        controller.archiveCategory(householdId, categoryId)

        assertEquals(CreateCall(userId, householdId, "Bakery"), service.createCalls.single())
        assertEquals(UpdateCall(userId, householdId, categoryId, "Bread"), service.updateCalls.single())
        assertEquals(ArchiveCall(userId, householdId, categoryId), service.archiveCalls.single())
    }

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }

    private class RecordingCategoryService(
        private val categories: List<Category>
    ) : ICategoryService {
        val getCalls = mutableListOf<GetCall>()
        val createCalls = mutableListOf<CreateCall>()
        val updateCalls = mutableListOf<UpdateCall>()
        val archiveCalls = mutableListOf<ArchiveCall>()

        override fun getCategories(userId: UUID, householdId: UUID, includeArchived: Boolean): List<Category> {
            getCalls += GetCall(userId, householdId, includeArchived)
            return categories
        }

        override fun createCategory(userId: UUID, householdId: UUID, name: String): Category {
            createCalls += CreateCall(userId, householdId, name)
            return Category(householdId = householdId, name = name)
        }

        override fun updateCategory(userId: UUID, householdId: UUID, categoryId: UUID, name: String): Category {
            updateCalls += UpdateCall(userId, householdId, categoryId, name)
            return Category(id = categoryId, householdId = householdId, name = name)
        }

        override fun archiveCategory(userId: UUID, householdId: UUID, categoryId: UUID) {
            archiveCalls += ArchiveCall(userId, householdId, categoryId)
        }
    }

    private data class GetCall(
        val userId: UUID,
        val householdId: UUID,
        val includeArchived: Boolean
    )

    private data class CreateCall(
        val userId: UUID,
        val householdId: UUID,
        val name: String
    )

    private data class UpdateCall(
        val userId: UUID,
        val householdId: UUID,
        val categoryId: UUID,
        val name: String
    )

    private data class ArchiveCall(
        val userId: UUID,
        val householdId: UUID,
        val categoryId: UUID
    )
}
