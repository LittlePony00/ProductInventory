package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.model.NotificationType
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSettingsRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReminderServiceImplTest {

    @Test
    fun `household reminder scan creates expiring and low stock notifications for every member`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val today = LocalDate.of(2026, 5, 14)
        val expiring = product(
            name = "Milk",
            householdId = householdId,
            expirationDate = today.plusDays(1)
        )
        val lowStock = product(
            name = "Rice",
            householdId = householdId,
            remainingAmount = 0.2,
            lowStockThreshold = 0.5
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = service(
            products = listOf(expiring, lowStock),
            memberships = listOf(
                Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER),
                Membership(userId = otherUserId, householdId = householdId, role = MembershipRole.MEMBER)
            ),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender
        )

        val result = service.runHouseholdDueReminders(userId, householdId, today)

        assertEquals(1, result.expiringProducts)
        assertEquals(1, result.lowStockProducts)
        assertEquals(4, result.notificationsCreated)
        assertEquals(4, notificationRepository.savedNotifications.size)
        assertEquals(4, notificationSender.sentPushes.size)
        assertTrue(
            notificationRepository.savedNotifications.any {
                it.userId == userId &&
                    it.type == NotificationType.REMINDER_EXPIRING_SOON &&
                    it.productId == expiring.id
            }
        )
        assertTrue(
            notificationRepository.savedNotifications.any {
                it.userId == otherUserId &&
                    it.type == NotificationType.REMINDER_LOW_STOCK &&
                    it.productId == lowStock.id
            }
        )
    }

    @Test
    fun `reminder scan does not duplicate existing product reminders`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val today = LocalDate.of(2026, 5, 14)
        val product = product(
            name = "Yogurt",
            householdId = householdId,
            expirationDate = today.plusDays(1)
        )
        val notificationRepository = RecordingNotificationRepository()
        val service = service(
            products = listOf(product),
            memberships = listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER)),
            notificationRepository = notificationRepository
        )

        val first = service.runHouseholdDueReminders(userId, householdId, today)
        val second = service.runHouseholdDueReminders(userId, householdId, today)

        assertEquals(1, first.notificationsCreated)
        assertEquals(0, second.notificationsCreated)
        assertEquals(1, notificationRepository.savedNotifications.size)
    }

    @Test
    fun `household reminder scan rejects non members`() {
        val service = service(products = emptyList(), memberships = emptyList())

        assertFailsWith<AccessDeniedException> {
            service.runHouseholdDueReminders(UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 5, 14))
        }
    }

    @Test
    fun `low stock reminders disabled suppresses low stock notification`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val today = LocalDate.of(2026, 5, 14)
        val lowStock = product(
            name = "Rice",
            householdId = householdId,
            remainingAmount = 0.2,
            lowStockThreshold = 0.5
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = service(
            products = listOf(lowStock),
            memberships = listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER)),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            settingsByUser = mapOf(
                userId to NotificationSettings(
                    userId = userId,
                    lowStockRemindersEnabled = false
                )
            )
        )

        val result = service.runHouseholdDueReminders(userId, householdId, today)

        assertEquals(1, result.lowStockProducts)
        assertEquals(0, result.notificationsCreated)
        assertEquals(0, notificationRepository.savedNotifications.size)
        assertEquals(0, notificationSender.sentPushes.size)
    }

    @Test
    fun `push disabled still creates in app reminder notification`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val today = LocalDate.of(2026, 5, 14)
        val expiring = product(
            name = "Milk",
            householdId = householdId,
            expirationDate = today.plusDays(1)
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = service(
            products = listOf(expiring),
            memberships = listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER)),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            settingsByUser = mapOf(userId to NotificationSettings(userId = userId, pushEnabled = false))
        )

        val result = service.runHouseholdDueReminders(userId, householdId, today)

        assertEquals(1, result.notificationsCreated)
        assertEquals(1, notificationRepository.savedNotifications.size)
        assertEquals(0, notificationSender.sentPushes.size)
    }

    private fun service(
        products: List<Product>,
        memberships: List<Membership>,
        notificationRepository: RecordingNotificationRepository = RecordingNotificationRepository(),
        notificationSender: RecordingNotificationSender = RecordingNotificationSender(),
        settingsByUser: Map<UUID, NotificationSettings> = emptyMap()
    ): ReminderServiceImpl =
        ReminderServiceImpl(
            productRepository = InMemoryProductRepository(products),
            membershipRepository = FakeMembershipRepository(memberships),
            notificationRepository = notificationRepository,
            notificationSettingsRepository = FakeNotificationSettingsRepository(settingsByUser),
            notificationSender = notificationSender
        )

    private fun product(
        name: String,
        householdId: UUID,
        expirationDate: LocalDate? = null,
        remainingAmount: Double = 1.0,
        lowStockThreshold: Double? = null
    ): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.let { com.android.rut.miit.productinventory.domain.model.ExpirationDate(it) },
            householdId = householdId,
            addedByUserId = UUID.randomUUID()
        )

    private class InMemoryProductRepository(private val products: List<Product>) : IProductRepository {
        override fun findById(id: UUID): Product? = products.firstOrNull { it.id == id }
        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
            products.firstOrNull { it.barcode == barcode && it.householdId == householdId }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
            products.filter { it.householdId == householdId && it.categoryId == categoryId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> =
            products.filter { it.householdId == householdId && it.expirationDate?.date?.isBefore(date) == true }

        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
            products.filter { product ->
                product.expirationDate?.date?.let { !it.isBefore(startInclusive) && it.isBefore(endExclusive) } == true
            }

        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> =
            findExpiringBetween(startInclusive, endExclusive).filter { it.householdId == householdId }

        override fun findLowStock(): List<Product> =
            products.filter { product ->
                product.lowStockThreshold?.let { product.remainingAmount <= it } == true
            }

        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> =
            findLowStock().filter { it.householdId == householdId }

        override fun save(product: Product): Product = product
        override fun deleteById(id: UUID) = Unit
        override fun existsById(id: UUID): Boolean = products.any { it.id == id }
    }

    private class FakeMembershipRepository(private val memberships: List<Membership>) : IMembershipRepository {
        override fun findByUserId(userId: UUID): List<Membership> =
            memberships.filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            memberships.filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership = membership
        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
    }

    private class RecordingNotificationRepository : INotificationRepository {
        val savedNotifications = mutableListOf<Notification>()

        override fun findByUserId(userId: UUID): List<Notification> =
            savedNotifications.filter { it.userId == userId }

        override fun findUnreadByUserId(userId: UUID): List<Notification> =
            savedNotifications.filter { it.userId == userId && !it.isRead }

        override fun existsByUserIdAndDedupeKey(userId: UUID, dedupeKey: String): Boolean =
            savedNotifications.any { it.userId == userId && it.dedupeKey == dedupeKey }

        override fun save(notification: Notification): Notification {
            savedNotifications += notification
            return notification
        }

        override fun markAsRead(id: UUID, userId: UUID) = Unit
        override fun markAllAsRead(userId: UUID) = Unit
    }

    private class FakeNotificationSettingsRepository(
        private val settingsByUser: Map<UUID, NotificationSettings>
    ) : INotificationSettingsRepository {
        override fun findByUserId(userId: UUID): NotificationSettings? = settingsByUser[userId]
        override fun save(settings: NotificationSettings): NotificationSettings = settings
    }

    private class RecordingNotificationSender : INotificationSender {
        val sentPushes = mutableListOf<Push>()

        override fun sendPush(userId: UUID, title: String, message: String) {
            sentPushes += Push(userId, title, message)
        }
    }

    private data class Push(
        val userId: UUID,
        val title: String,
        val message: String
    )
}
