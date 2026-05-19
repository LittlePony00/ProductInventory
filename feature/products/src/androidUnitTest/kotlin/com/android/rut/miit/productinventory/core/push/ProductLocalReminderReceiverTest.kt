package com.android.rut.miit.productinventory.core.push

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminderPlanner
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductLocalReminderReceiverTest {

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        productLocalReminderBootDispatcher = Dispatchers.IO
    }

    @Test
    fun `boot completed clears stale ids and reschedules reminders from local state`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(SCHEDULED_IDS_KEY, setOf("stale-reminder"))
            .commit()
        val rescheduler = AndroidProductLocalReminderRescheduler(
            householdLocalDataSource = FakeHouseholdLocalDataSource(),
            productLocalDataSource = FakeProductLocalDataSource(),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(FakeNotificationRepository())
        )

        context.rescheduleProductLocalRemindersAfterBoot(rescheduler)

        val scheduledIds = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(SCHEDULED_IDS_KEY, emptySet())
            .orEmpty()
        assertTrue("stale-reminder" !in scheduledIds)
        assertTrue(scheduledIds.any { it.startsWith("expiration:household-id:product-id") })
    }

    @Test
    fun `boot completed reschedules persisted reminders without repository rebuild`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reminder = ProductLocalReminderPlanner()
            .plan(listOf(product("household-id")), NotificationSettings(expirationReminderDays = 2))
            .single()
        context.scheduleProductLocalReminders(listOf(reminder))
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(SCHEDULED_IDS_KEY, setOf("stale-reminder"))
            .commit()

        val restored = context.reschedulePersistedProductLocalRemindersAfterBoot()

        val scheduledIds = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(SCHEDULED_IDS_KEY, emptySet())
            .orEmpty()
        assertTrue(restored)
        assertTrue(reminder.id in scheduledIds)
        assertTrue("stale-reminder" !in scheduledIds)
    }

    @Test
    fun `boot completed returns false when no persisted reminders exist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val restored = context.reschedulePersistedProductLocalRemindersAfterBoot()

        assertFalse(restored)
    }

    private class FakeHouseholdLocalDataSource : HouseholdLocalDataSource {
        override suspend fun getHouseholds(): List<Household> =
            listOf(Household(id = "household-id", name = "Home", createdAt = "2026-05-19T00:00:00Z"))
        override suspend fun saveHouseholds(households: List<Household>) = Unit
        override suspend fun getHouseholdById(id: String): Household? = null
    }

    private class FakeProductLocalDataSource : ProductLocalDataSource {
        override suspend fun getProducts(householdId: String): List<Product> =
            listOf(product(householdId))
        override suspend fun getProduct(householdId: String, id: String): Product? = null
        override suspend fun saveProducts(householdId: String, products: List<Product>) = Unit
        override suspend fun getProductByBarcode(householdId: String, barcode: String): Product? = null
        override suspend fun remapCategoryId(
            householdId: String,
            oldCategoryId: String,
            newCategoryId: String,
            newCategoryName: String
        ) = Unit
        override suspend fun deleteProduct(id: String) = Unit
        override suspend fun saveProduct(product: Product) = Unit
    }

    private class FakeNotificationRepository : NotificationRepository {
        override suspend fun getNotifications(): List<Notification> = emptyList()
        override suspend fun getUnreadNotifications(): List<Notification> = emptyList()
        override suspend fun markAsRead(notificationId: String) = Unit
        override suspend fun markAllAsRead() = Unit
        override suspend fun getSettings(): NotificationSettings = NotificationSettings(expirationReminderDays = 2)
        override suspend fun updateSettings(
            expirationRemindersEnabled: Boolean?,
            lowStockRemindersEnabled: Boolean?,
            pushEnabled: Boolean?,
            expirationReminderDays: Int?
        ): NotificationSettings = getSettings()
    }

    private companion object {
        const val PREFERENCES = "product_local_reminders"
        const val SCHEDULED_IDS_KEY = "scheduled_local_reminder_ids"

        fun product(householdId: String) = Product(
            id = "product-id",
            name = "Milk",
            category = ProductCategory.DAIRY,
            categoryId = null,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-25"),
            expirationStatus = ExpirationStatus.FRESH,
            householdId = householdId,
            addedByUserId = "user-id",
            createdAt = "2026-05-19T00:00:00Z"
        )
    }
}
