package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.LocalDate
import java.util.UUID

class ProductServiceImplTest {

    @Test
    fun `add product persists extended fields and defaults remaining amount to original quantity`() {
        val actorId = UUID.randomUUID()
        val otherMemberId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productRepository = InMemoryProductRepository()
        val membershipRepository = FakeMembershipRepository(
            memberships = listOf(
                Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER),
                Membership(userId = otherMemberId, householdId = householdId, role = MembershipRole.MEMBER)
            )
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = membershipRepository,
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            householdEventPublisher = RecordingHouseholdEventPublisher()
        )
        val purchaseDate = LocalDate.of(2026, 5, 14)

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = " Milk ",
            brand = " Brand ",
            barcode = " 4601234567890 ",
            category = ProductCategory.DAIRY,
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            packageAmount = 950.0,
            packageUnit = QuantityUnit.MILLILITERS,
            ingredientsText = " Milk ",
            calories = 60.0,
            protein = 3.0,
            fat = 2.5,
            carbs = 4.7,
            purchaseDate = purchaseDate,
            remainingAmount = null,
            lowStockThreshold = 0.5,
            expirationDate = null
        )

        assertEquals("Milk", product.name)
        assertEquals("Brand", product.brand)
        assertEquals("4601234567890", product.barcode)
        assertEquals(950.0, product.packageQuantity?.value)
        assertEquals(QuantityUnit.MILLILITERS, product.packageQuantity?.unit)
        assertEquals("Milk", product.ingredientsText)
        assertEquals(60.0, product.calories)
        assertEquals(3.0, product.protein)
        assertEquals(2.5, product.fat)
        assertEquals(4.7, product.carbs)
        assertEquals(purchaseDate, product.purchaseDate)
        assertEquals(2.0, product.remainingAmount)
        assertEquals(0.5, product.lowStockThreshold)
        assertEquals(product, productRepository.savedProducts.single())
        assertEquals(otherMemberId, notificationRepository.savedNotifications.single().userId)
        assertEquals(otherMemberId, notificationSender.sentPushes.single().userId)
    }

    @Test
    fun `update product changes extended fields and preserves omitted fields`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            brand = "Old brand",
            barcode = "111",
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            packageQuantity = Quantity(1_000.0, QuantityUnit.GRAMS),
            ingredientsText = "Rice",
            calories = 350.0,
            protein = 7.0,
            fat = 1.0,
            carbs = 78.0,
            purchaseDate = LocalDate.of(2026, 5, 1),
            remainingAmount = 0.75,
            lowStockThreshold = 0.2,
            householdId = householdId,
            addedByUserId = actorId
        )
        val productRepository = InMemoryProductRepository(initialProducts = listOf(existing))
        val service = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher()
        )

        val updated = service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = " New brand ",
            barcode = null,
            category = null,
            quantity = 2.0,
            quantityUnit = null,
            packageAmount = null,
            packageUnit = QuantityUnit.PIECES,
            ingredientsText = null,
            calories = null,
            protein = 8.0,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = 1.5,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(existing.name, updated.name)
        assertEquals("New brand", updated.brand)
        assertEquals(existing.barcode, updated.barcode)
        assertEquals(2.0, updated.quantity.value)
        assertEquals(existing.quantity.unit, updated.quantity.unit)
        assertEquals(existing.packageQuantity?.value, updated.packageQuantity?.value)
        assertEquals(QuantityUnit.PIECES, updated.packageQuantity?.unit)
        assertEquals(existing.ingredientsText, updated.ingredientsText)
        assertEquals(existing.calories, updated.calories)
        assertEquals(8.0, updated.protein)
        assertEquals(existing.fat, updated.fat)
        assertEquals(existing.carbs, updated.carbs)
        assertEquals(existing.purchaseDate, updated.purchaseDate)
        assertEquals(1.5, updated.remainingAmount)
        assertEquals(existing.lowStockThreshold, updated.lowStockThreshold)
    }

    @Test
    fun `add product uses quantity unit for package unit when package unit is omitted`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher()
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Juice",
            brand = null,
            barcode = null,
            category = ProductCategory.BEVERAGES,
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            packageAmount = 1_000.0,
            packageUnit = null,
            ingredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(1_000.0, product.packageQuantity?.value)
        assertEquals(QuantityUnit.PIECES, product.packageQuantity?.unit)
    }

    @Test
    fun `add product publishes created low inventory and expiring soon events`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Yogurt",
            brand = null,
            barcode = null,
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            packageAmount = null,
            packageUnit = null,
            ingredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = 0.25,
            lowStockThreshold = 0.5,
            expirationDate = LocalDate.now().plusDays(1)
        )

        assertEquals(
            listOf(
                HouseholdEventType.PRODUCT_CREATED,
                HouseholdEventType.INVENTORY_LOW,
                HouseholdEventType.EXPIRING_SOON
            ),
            eventPublisher.events.map { it.type }
        )
        eventPublisher.events.forEach { event ->
            assertEquals(householdId, event.householdId)
            assertEquals(actorId, event.actorUserId)
            assertEquals(product.id, event.productId)
            assertEquals("Yogurt", event.productName)
        }
    }

    @Test
    fun `update product publishes updated and category changed events`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = null,
            barcode = null,
            category = ProductCategory.OTHER,
            quantity = null,
            quantityUnit = null,
            packageAmount = null,
            packageUnit = null,
            ingredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(
            listOf(HouseholdEventType.PRODUCT_UPDATED, HouseholdEventType.CATEGORY_CHANGED),
            eventPublisher.events.map { it.type }
        )
        val categoryChanged = eventPublisher.events.last()
        assertEquals(ProductCategory.CEREALS, categoryChanged.previousCategory)
        assertEquals(ProductCategory.OTHER, categoryChanged.currentCategory)
    }

    @Test
    fun `update product publishes low inventory and expiring soon only when entering those states`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Milk",
            category = ProductCategory.DAIRY,
            quantity = Quantity(2.0, QuantityUnit.PIECES),
            remainingAmount = 2.0,
            lowStockThreshold = 0.5,
            expirationDate = ExpirationDate(LocalDate.now().plusDays(7)),
            householdId = householdId,
            addedByUserId = actorId
        )
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = null,
            barcode = null,
            category = null,
            quantity = null,
            quantityUnit = null,
            packageAmount = null,
            packageUnit = null,
            ingredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = 0.25,
            lowStockThreshold = null,
            expirationDate = LocalDate.now().plusDays(1)
        )

        assertEquals(
            listOf(
                HouseholdEventType.PRODUCT_UPDATED,
                HouseholdEventType.INVENTORY_LOW,
                HouseholdEventType.EXPIRING_SOON
            ),
            eventPublisher.events.map { it.type }
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = "Milk updated",
            brand = null,
            barcode = null,
            category = null,
            quantity = null,
            quantityUnit = null,
            packageAmount = null,
            packageUnit = null,
            ingredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(
            listOf(
                HouseholdEventType.PRODUCT_UPDATED,
                HouseholdEventType.INVENTORY_LOW,
                HouseholdEventType.EXPIRING_SOON,
                HouseholdEventType.PRODUCT_UPDATED
            ),
            eventPublisher.events.map { it.type }
        )
    }

    @Test
    fun `delete product publishes deleted event`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        service.deleteProduct(actorId, existing.id)

        val event = eventPublisher.events.single()
        assertEquals(HouseholdEventType.PRODUCT_DELETED, event.type)
        assertEquals(existing.id, event.productId)
        assertEquals(householdId, event.householdId)
    }

    private class InMemoryProductRepository(
        initialProducts: List<Product> = emptyList()
    ) : IProductRepository {
        private val products = initialProducts.associateBy { it.id }.toMutableMap()
        val savedProducts = mutableListOf<Product>()

        override fun findById(id: UUID): Product? = products[id]

        override fun findFirstByBarcode(barcode: String): Product? =
            products.values.firstOrNull { it.barcode == barcode }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.values.filter { it.householdId == householdId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()

        override fun save(product: Product): Product {
            products[product.id] = product
            savedProducts += product
            return product
        }

        override fun deleteById(id: UUID) {
            products.remove(id)
        }

        override fun existsById(id: UUID): Boolean = products.containsKey(id)
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

    private class RecordingNotificationRepository : INotificationRepository {
        val savedNotifications = mutableListOf<Notification>()

        override fun findByUserId(userId: UUID): List<Notification> = emptyList()

        override fun findUnreadByUserId(userId: UUID): List<Notification> = emptyList()

        override fun save(notification: Notification): Notification {
            savedNotifications += notification
            return notification
        }

        override fun markAsRead(id: UUID, userId: UUID) = Unit

        override fun markAllAsRead(userId: UUID) = Unit
    }

    private class RecordingNotificationSender : INotificationSender {
        val sentPushes = mutableListOf<Push>()

        override fun sendPush(userId: UUID, title: String, message: String) {
            sentPushes += Push(userId, title, message)
        }
    }

    private class RecordingHouseholdEventPublisher : IHouseholdEventPublisher {
        val events = mutableListOf<HouseholdEvent>()

        override fun publish(event: HouseholdEvent) {
            events += event
        }
    }

    private data class Push(
        val userId: UUID,
        val title: String,
        val message: String
    )
}
