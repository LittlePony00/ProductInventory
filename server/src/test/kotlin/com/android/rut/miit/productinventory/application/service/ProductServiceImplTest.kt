package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )
        val purchaseDate = LocalDate.of(2026, 5, 14)

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = " Milk ",
            brand = " Brand ",
            barcode = " 4601234567890 ",
            category = ProductCategory.DAIRY,
            categoryId = null,
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
        assertEquals(
            setOf(actorId, otherMemberId),
            notificationRepository.savedNotifications.map { it.userId }.toSet()
        )
        assertEquals(
            setOf(actorId, otherMemberId),
            notificationSender.sentPushes.map { it.userId }.toSet()
        )
    }

    @Test
    fun `add product notifies actor user so other devices of same account receive the event`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Milk",
            brand = null,
            barcode = null,
            category = ProductCategory.DAIRY,
            categoryId = null,
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
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(actorId, notificationRepository.savedNotifications.single().userId)
        assertEquals(actorId, notificationSender.sentPushes.single().userId)
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
            categoryId = null,
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
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val updated = service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = " New brand ",
            barcode = null,
            category = null,
            categoryId = null,
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
    fun `update product notifies all household members with backend notification ids`() {
        val actorId = UUID.randomUUID()
        val otherMemberId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            categoryId = null,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(
                    Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER),
                    Membership(userId = otherMemberId, householdId = householdId, role = MembershipRole.MEMBER)
                )
            ),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = "Rice updated",
            brand = null,
            barcode = null,
            category = null,
            categoryId = null,
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

        assertEquals(setOf(actorId, otherMemberId), notificationRepository.savedNotifications.map { it.userId }.toSet())
        notificationRepository.savedNotifications.forEach { notification ->
            assertEquals("Продукт изменён", notification.title)
            assertEquals("«Rice updated» изменён", notification.message)
        }
        assertEquals(
            notificationRepository.savedNotifications.map { it.id }.toSet(),
            notificationSender.sentPushes.map { it.notificationId }.toSet()
        )
        assertEquals(setOf(actorId, otherMemberId), notificationSender.sentPushes.map { it.userId }.toSet())
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
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Juice",
            brand = null,
            barcode = null,
            category = ProductCategory.BEVERAGES,
            categoryId = null,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Yogurt",
            brand = null,
            barcode = null,
            category = ProductCategory.DAIRY,
            categoryId = null,
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
            categoryId = null,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = null,
            barcode = null,
            category = ProductCategory.OTHER,
            categoryId = null,
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
            categoryId = null,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = null,
            brand = null,
            barcode = null,
            category = null,
            categoryId = null,
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
            categoryId = null,
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
    fun `add product with custom category stores category id and name`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val customCategory = Category(householdId = householdId, name = "Bakery")
        val productRepository = InMemoryProductRepository()
        val service = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(
                (SystemCategoryCatalog.categories + customCategory).toMutableList()
            ),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Bread",
            brand = null,
            barcode = null,
            category = ProductCategory.OTHER,
            categoryId = customCategory.id,
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
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        assertEquals(customCategory.id, product.categoryId)
        assertEquals("Bakery", product.categoryName)
        assertEquals(ProductCategory.OTHER, product.category)
        assertEquals(customCategory.id, productRepository.savedProducts.single().categoryId)
    }

    @Test
    fun `get products filters by category id and enriches category details`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val bakeryCategory = Category(householdId = householdId, name = "Bakery")
        val pantryCategory = Category(householdId = householdId, name = "Pantry")
        val bread = Product(
            id = UUID.randomUUID(),
            name = "Bread",
            category = ProductCategory.OTHER,
            categoryId = bakeryCategory.id,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val rice = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            categoryId = pantryCategory.id,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(bread, rice)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(
                (SystemCategoryCatalog.categories + bakeryCategory + pantryCategory).toMutableList()
            ),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val products = service.getProducts(actorId, householdId, bakeryCategory.id)

        assertEquals(listOf("Bread"), products.map { it.name })
        assertEquals(bakeryCategory.id, products.single().categoryId)
        assertEquals("Bakery", products.single().categoryName)
    }

    @Test
    fun `delete product publishes deleted event`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            categoryId = null,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.deleteProduct(actorId, existing.id)

        val event = eventPublisher.events.single()
        assertEquals(HouseholdEventType.PRODUCT_DELETED, event.type)
        assertEquals(existing.id, event.productId)
        assertEquals(householdId, event.householdId)
    }

    @Test
    fun `delete product notifies all household members with backend notification ids`() {
        val actorId = UUID.randomUUID()
        val otherMemberId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            categoryId = null,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val notificationRepository = RecordingNotificationRepository()
        val notificationSender = RecordingNotificationSender()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(
                    Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER),
                    Membership(userId = otherMemberId, householdId = householdId, role = MembershipRole.MEMBER)
                )
            ),
            notificationRepository = notificationRepository,
            notificationSender = notificationSender,
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        service.deleteProduct(actorId, existing.id)

        assertEquals(setOf(actorId, otherMemberId), notificationRepository.savedNotifications.map { it.userId }.toSet())
        notificationRepository.savedNotifications.forEach { notification ->
            assertEquals("Продукт удалён", notification.title)
            assertEquals("«Rice» удалён", notification.message)
        }
        assertEquals(
            notificationRepository.savedNotifications.map { it.id }.toSet(),
            notificationSender.sentPushes.map { it.notificationId }.toSet()
        )
        assertEquals(setOf(actorId, otherMemberId), notificationSender.sentPushes.map { it.userId }.toSet())
    }

    @Test
    fun `consume product decreases remaining amount and publishes quantity changed event`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Milk",
            category = ProductCategory.DAIRY,
            quantity = Quantity(2.0, QuantityUnit.PIECES),
            remainingAmount = 2.0,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val product = service.consumeProduct(actorId, existing.id, 0.75)

        assertEquals(1.25, product.remainingAmount)
        assertEquals(listOf(HouseholdEventType.PRODUCT_QUANTITY_CHANGED), eventPublisher.events.map { it.type })
        assertEquals(existing.id, eventPublisher.events.single().productId)
    }

    @Test
    fun `consume product publishes depleted event when remaining amount reaches zero`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Yogurt",
            category = ProductCategory.DAIRY,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = 1.0,
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
            householdEventPublisher = eventPublisher,
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        val product = service.consumeProduct(actorId, existing.id, 1.0)

        assertEquals(0.0, product.remainingAmount)
        assertEquals(
            listOf(HouseholdEventType.PRODUCT_QUANTITY_CHANGED, HouseholdEventType.PRODUCT_DEPLETED),
            eventPublisher.events.map { it.type }
        )
    }

    @Test
    fun `consume product rejects amount above remaining amount`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = 0.5,
            householdId = householdId,
            addedByUserId = actorId
        )
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        assertFailsWith<IllegalArgumentException> {
            service.consumeProduct(actorId, existing.id, 0.75)
        }
    }

    @Test
    fun `product access rejects users outside household`() {
        val actorId = UUID.randomUUID()
        val nonMemberId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Rice",
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = 1.0,
            householdId = householdId,
            addedByUserId = actorId
        )
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = RecordingBarcodeProductCacheRepository()
        )

        assertFailsWith<AccessDeniedException> {
            service.getProducts(nonMemberId, householdId, null)
        }
        assertFailsWith<AccessDeniedException> {
            service.getProduct(nonMemberId, existing.id)
        }
        assertFailsWith<AccessDeniedException> {
            service.getExpiringProducts(nonMemberId, householdId, days = 3)
        }
        assertFailsWith<AccessDeniedException> {
            service.consumeProduct(nonMemberId, existing.id, amount = 0.5)
        }
        assertFailsWith<AccessDeniedException> {
            service.deleteProduct(nonMemberId, existing.id)
        }
    }

    @Test
    fun `add product with barcode saves reusable local barcode metadata`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val cacheRepository = RecordingBarcodeProductCacheRepository()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = cacheRepository
        )

        service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = " Custom Milk ",
            brand = " Local Brand ",
            barcode = " 4601234567890 ",
            category = ProductCategory.DAIRY,
            categoryId = null,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            packageAmount = 950.0,
            packageUnit = QuantityUnit.MILLILITERS,
            ingredientsText = " Milk ",
            calories = 60.0,
            protein = 3.0,
            fat = 2.5,
            carbs = 4.7,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        val cachedDraft = cacheRepository.findByBarcode("4601234567890")
        assertEquals(BarcodeProductSource.LOCAL_DATABASE, cachedDraft?.source)
        assertEquals("Custom Milk", cachedDraft?.name)
        assertEquals("Local Brand", cachedDraft?.brand)
        assertEquals(950.0, cachedDraft?.packageQuantity?.value)
        assertEquals(QuantityUnit.MILLILITERS, cachedDraft?.packageQuantity?.unit)
        assertEquals(ProductCategory.DAIRY, cachedDraft?.category)
    }

    @Test
    fun `update product with barcode refreshes reusable local barcode metadata`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val existing = Product(
            id = UUID.randomUUID(),
            name = "Old name",
            brand = "Old brand",
            barcode = "4601234567890",
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = householdId,
            addedByUserId = actorId
        )
        val cacheRepository = RecordingBarcodeProductCacheRepository()
        val service = ProductServiceImpl(
            productRepository = InMemoryProductRepository(initialProducts = listOf(existing)),
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = cacheRepository
        )

        service.updateProduct(
            userId = actorId,
            productId = existing.id,
            name = " Fresh Local Name ",
            brand = " Fresh Brand ",
            barcode = null,
            category = ProductCategory.DAIRY,
            categoryId = null,
            quantity = null,
            quantityUnit = null,
            packageAmount = null,
            packageUnit = null,
            ingredientsText = " Milk ",
            calories = 70.0,
            protein = null,
            fat = null,
            carbs = null,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        val cachedDraft = cacheRepository.findByBarcode("4601234567890")
        assertEquals("Fresh Local Name", cachedDraft?.name)
        assertEquals("Fresh Brand", cachedDraft?.brand)
        assertEquals(ProductCategory.DAIRY, cachedDraft?.category)
        assertEquals("Milk", cachedDraft?.ingredients)
        assertEquals(70.0, cachedDraft?.nutrition?.caloriesKcal)
    }

    @Test
    fun `delete product keeps reusable local barcode metadata`() {
        val actorId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val cacheRepository = RecordingBarcodeProductCacheRepository()
        val productRepository = InMemoryProductRepository()
        val service = ProductServiceImpl(
            productRepository = productRepository,
            membershipRepository = FakeMembershipRepository(
                memberships = listOf(Membership(userId = actorId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = RecordingHouseholdEventPublisher(),
            categoryRepository = FakeCategoryRepository(),
            barcodeProductCacheRepository = cacheRepository
        )

        val product = service.addProduct(
            userId = actorId,
            householdId = householdId,
            name = "Persistent Draft",
            brand = null,
            barcode = "4601234567890",
            category = ProductCategory.DAIRY,
            categoryId = null,
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
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        )

        service.deleteProduct(actorId, product.id)

        assertEquals(false, productRepository.existsById(product.id))
        assertEquals("Persistent Draft", cacheRepository.findByBarcode("4601234567890")?.name)
    }

    private class InMemoryProductRepository(
        initialProducts: List<Product> = emptyList()
    ) : IProductRepository {
        private val products = initialProducts.associateBy { it.id }.toMutableMap()
        val savedProducts = mutableListOf<Product>()

        override fun findById(id: UUID): Product? = products[id]

        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
            products.values.firstOrNull { it.barcode == barcode && it.householdId == householdId }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.values.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
            products.values.filter { it.householdId == householdId && it.categoryId == categoryId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()

        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
            products.values.filter { product ->
                product.expirationDate?.date?.let { !it.isBefore(startInclusive) && it.isBefore(endExclusive) } == true
            }

        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> =
            findExpiringBetween(startInclusive, endExclusive).filter { it.householdId == householdId }

        override fun findLowStock(): List<Product> =
            products.values.filter { product ->
                product.lowStockThreshold?.let { product.remainingAmount <= it } == true
            }

        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> =
            findLowStock().filter { it.householdId == householdId }

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

    private class FakeCategoryRepository(
        private val categories: MutableList<Category> = SystemCategoryCatalog.categories.toMutableList()
    ) : ICategoryRepository {
        override fun findSystemCategories(includeArchived: Boolean): List<Category> =
            categories.filter { it.system && (includeArchived || !it.archived) }

        override fun findByHouseholdId(householdId: UUID, includeArchived: Boolean): List<Category> =
            categories.filter { it.householdId == householdId && (includeArchived || !it.archived) }

        override fun findAvailableById(categoryId: UUID, householdId: UUID): Category? =
            categories.firstOrNull {
                it.id == categoryId && !it.archived && (it.system || it.householdId == householdId)
            }

        override fun save(category: Category): Category {
            categories.removeAll { it.id == category.id }
            categories += category
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

    private class RecordingNotificationRepository : INotificationRepository {
        val savedNotifications = mutableListOf<Notification>()

        override fun findByUserId(userId: UUID): List<Notification> = emptyList()

        override fun findUnreadByUserId(userId: UUID): List<Notification> = emptyList()

        override fun existsByUserIdAndDedupeKey(userId: UUID, dedupeKey: String): Boolean =
            savedNotifications.any { it.userId == userId && it.dedupeKey == dedupeKey }

        override fun save(notification: Notification): Notification {
            savedNotifications += notification
            return notification
        }

        override fun markAsRead(id: UUID, userId: UUID) = Unit

        override fun markAllAsRead(userId: UUID) = Unit
    }

    private class RecordingNotificationSender : INotificationSender {
        val sentPushes = mutableListOf<Push>()

        override fun sendPush(userId: UUID, title: String, message: String, notificationId: UUID?) {
            sentPushes += Push(userId, title, message, notificationId)
        }
    }

    private class RecordingHouseholdEventPublisher : IHouseholdEventPublisher {
        val events = mutableListOf<HouseholdEvent>()

        override fun publish(event: HouseholdEvent) {
            events += event
        }
    }

    private class RecordingBarcodeProductCacheRepository : IBarcodeProductCacheRepository {
        private val drafts = mutableMapOf<String, BarcodeProductDraft>()

        override fun findByBarcode(barcode: String): BarcodeProductDraft? = drafts[barcode]

        override fun save(draft: BarcodeProductDraft): BarcodeProductDraft {
            drafts[draft.barcode] = draft
            return draft
        }
    }

    private data class Push(
        val userId: UUID,
        val title: String,
        val message: String,
        val notificationId: UUID?
    )
}
