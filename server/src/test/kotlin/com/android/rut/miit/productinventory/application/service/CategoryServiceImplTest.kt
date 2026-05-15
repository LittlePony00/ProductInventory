package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.exception.DomainException
import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryServiceImplTest {

    @Test
    fun `get categories returns system and household categories`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val category = Category(householdId = householdId, name = "Bakery")
        val service = service(
            userId = userId,
            householdId = householdId,
            categories = listOf(category)
        )

        val categories = service.getCategories(userId, householdId, includeArchived = false)

        assertEquals(SystemCategoryCatalog.categories.size + 1, categories.size)
        assertTrue(categories.any { it.id == SystemCategoryCatalog.dairyId && it.system })
        assertTrue(categories.any { it.id == category.id && it.name == "Bakery" })
    }

    @Test
    fun `archive hides custom category unless archived included`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val category = Category(householdId = householdId, name = "Bakery")
        val service = service(
            userId = userId,
            householdId = householdId,
            categories = listOf(category)
        )

        service.archiveCategory(userId, householdId, category.id)

        assertFalse(service.getCategories(userId, householdId, includeArchived = false).any { it.id == category.id })
        assertTrue(service.getCategories(userId, householdId, includeArchived = true).any { it.id == category.id && it.archived })
    }

    @Test
    fun `create and update category are scoped to household`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val repository = InMemoryCategoryRepository()
        val service = CategoryServiceImpl(
            categoryRepository = repository,
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            )
        )

        val created = service.createCategory(userId, householdId, " Bakery ")
        val updated = service.updateCategory(userId, householdId, created.id, " Bread ")

        assertEquals(householdId, created.householdId)
        assertEquals("Bakery", created.name)
        assertEquals("Bread", updated.name)
        assertEquals(householdId, updated.householdId)
    }

    @Test
    fun `cannot edit or archive system category`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(userId = userId, householdId = householdId)

        assertFailsWith<DomainException> {
            service.updateCategory(userId, householdId, SystemCategoryCatalog.dairyId, "Milk")
        }
        assertFailsWith<DomainException> {
            service.archiveCategory(userId, householdId, SystemCategoryCatalog.dairyId)
        }
    }

    @Test
    fun `cannot access categories outside household`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val otherHouseholdId = UUID.randomUUID()
        val otherCategory = Category(householdId = otherHouseholdId, name = "Other")
        val service = service(
            userId = userId,
            householdId = householdId,
            categories = listOf(otherCategory)
        )

        assertFailsWith<EntityNotFoundException> {
            service.updateCategory(userId, householdId, otherCategory.id, "Updated")
        }
    }

    @Test
    fun `rejects user outside household`() {
        val service = CategoryServiceImpl(
            categoryRepository = InMemoryCategoryRepository(),
            membershipRepository = FakeMembershipRepository(emptyList())
        )

        assertFailsWith<AccessDeniedException> {
            service.getCategories(UUID.randomUUID(), UUID.randomUUID(), includeArchived = false)
        }
    }

    private fun service(
        userId: UUID,
        householdId: UUID,
        categories: List<Category> = emptyList()
    ): CategoryServiceImpl =
        CategoryServiceImpl(
            categoryRepository = InMemoryCategoryRepository(categories),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            )
        )

    private class InMemoryCategoryRepository(
        categories: List<Category> = emptyList()
    ) : ICategoryRepository {
        private val categories = (SystemCategoryCatalog.categories + categories).associateBy { it.id }.toMutableMap()

        override fun findSystemCategories(includeArchived: Boolean): List<Category> =
            categories.values.filter { it.system && (includeArchived || !it.archived) }

        override fun findByHouseholdId(householdId: UUID, includeArchived: Boolean): List<Category> =
            categories.values.filter { it.householdId == householdId && (includeArchived || !it.archived) }

        override fun findAvailableById(categoryId: UUID, householdId: UUID): Category? =
            categories[categoryId]?.takeIf { !it.archived && (it.system || it.householdId == householdId) }

        override fun save(category: Category): Category {
            categories[category.id] = category
            return category
        }
    }

    private class FakeMembershipRepository(
        private val memberships: List<Membership>
    ) : IMembershipRepository {
        override fun findByUserId(userId: UUID): List<Membership> =
            memberships.filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            memberships.filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership = membership

        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
    }
}
