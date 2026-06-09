package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.api.models.customCategoryNameForDisplay
import com.android.rut.miit.productinventory.feature.products.api.models.displayImage
import com.android.rut.miit.productinventory.feature.products.presentation.list.InventoryFilter
import com.android.rut.miit.productinventory.feature.products.presentation.list.ProductListFilters
import com.android.rut.miit.productinventory.feature.products.presentation.list.applyFilters
import com.android.rut.miit.productinventory.feature.products.presentation.list.isLowStock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class CompactProductContractTest {

    @Test fun `display image prefers local path`() = assertEquals("/local/milk.jpg", product(localImagePath = "/local/milk.jpg", imageUrl = "https://cdn/milk.jpg").displayImage)
    @Test fun `display image falls back to remote url`() = assertEquals("https://cdn/milk.jpg", product(imageUrl = "https://cdn/milk.jpg").displayImage)
    @Test fun `display image ignores blank local path`() = assertEquals("https://cdn/milk.jpg", product(localImagePath = " ", imageUrl = "https://cdn/milk.jpg").displayImage)
    @Test fun `display image ignores blank remote url`() = assertNull(product(imageUrl = " ").displayImage)
    @Test fun `display image is null without image sources`() = assertNull(product().displayImage)
    @Test fun `display image keeps non blank local path over remote`() = assertEquals("/cache/1.webp", product(localImagePath = "/cache/1.webp", imageUrl = "https://cdn/1.webp").displayImage)

    @Test fun `custom category name is shown for custom other category`() = assertEquals("Bakery", product(categoryId = "custom", categoryName = "Bakery").customCategoryNameForDisplay())
    @Test fun `custom category name is hidden for system other category`() = assertNull(product(categoryId = ProductCategoryOption.OTHER_SYSTEM_ID, categoryName = "Other").customCategoryNameForDisplay())
    @Test fun `custom category name is hidden without category id`() = assertNull(product(categoryId = null, categoryName = "Pantry").customCategoryNameForDisplay())
    @Test fun `custom category name is hidden without name`() = assertNull(product(categoryId = "custom", categoryName = null).customCategoryNameForDisplay())
    @Test fun `custom category name is hidden for dairy category`() = assertNull(product(category = ProductCategory.DAIRY, categoryId = "custom", categoryName = "Milk").customCategoryNameForDisplay())
    @Test fun `custom category name preserves non blank custom name`() = assertEquals("Freezer", product(categoryId = "freezer", categoryName = "Freezer").customCategoryNameForDisplay())

    @Test fun `system category list has six categories`() = assertEquals(6, ProductCategoryOption.systemDefaults().size)
    @Test fun `system dairy id is stable`() = assertSystem(ProductCategory.DAIRY, ProductCategoryOption.DAIRY_SYSTEM_ID)
    @Test fun `system meat fish id is stable`() = assertSystem(ProductCategory.MEAT_FISH, ProductCategoryOption.MEAT_FISH_SYSTEM_ID)
    @Test fun `system vegetables fruits id is stable`() = assertSystem(ProductCategory.VEGETABLES_FRUITS, ProductCategoryOption.VEGETABLES_FRUITS_SYSTEM_ID)
    @Test fun `system cereals id is stable`() = assertSystem(ProductCategory.CEREALS, ProductCategoryOption.CEREALS_SYSTEM_ID)
    @Test fun `system beverages id is stable`() = assertSystem(ProductCategory.BEVERAGES, ProductCategoryOption.BEVERAGES_SYSTEM_ID)
    @Test fun `system other id is stable`() = assertSystem(ProductCategory.OTHER, ProductCategoryOption.OTHER_SYSTEM_ID)
    @Test fun `system categories are not archived`() = assertTrue(ProductCategoryOption.systemDefaults().none { it.archived })
    @Test fun `system categories are marked system`() = assertTrue(ProductCategoryOption.systemDefaults().all { it.system })
    @Test fun `custom category legacy category is other`() = assertEquals(ProductCategory.OTHER, categoryOption(code = null).legacyCategory)
    @Test fun `system category legacy category follows code`() = assertEquals(ProductCategory.CEREALS, categoryOption(code = ProductCategory.CEREALS).legacyCategory)

    @Test fun `product is low stock below threshold`() = assertTrue(product(remainingAmount = 1.0, lowStockThreshold = 2.0).isLowStock)
    @Test fun `product is low stock at threshold`() = assertTrue(product(remainingAmount = 2.0, lowStockThreshold = 2.0).isLowStock)
    @Test fun `product is not low stock above threshold`() = assertFalse(product(remainingAmount = 3.0, lowStockThreshold = 2.0).isLowStock)
    @Test fun `product is not low stock without threshold`() = assertFalse(product(remainingAmount = 0.0, lowStockThreshold = null).isLowStock)
    @Test fun `zero remaining is low stock with zero threshold`() = assertTrue(product(remainingAmount = 0.0, lowStockThreshold = 0.0).isLowStock)
    @Test fun `negative remaining is low stock`() = assertTrue(product(remainingAmount = -1.0, lowStockThreshold = 0.0).isLowStock)

    @Test fun `all inventory filter keeps all products`() = assertIds(products("a", "b").applyFilters(ProductListFilters()), "a", "b")
    @Test fun `low stock filter keeps only low stock`() = assertIds(listOf(product("ok", remainingAmount = 5.0, lowStockThreshold = 2.0), product("low", remainingAmount = 1.0, lowStockThreshold = 2.0)).applyFilters(ProductListFilters(inventory = InventoryFilter.LOW_STOCK)), "low")
    @Test fun `expiring soon filter keeps expiring products`() = assertIds(listOf(product("fresh"), product("soon", expirationStatus = ExpirationStatus.EXPIRING_SOON)).applyFilters(ProductListFilters(inventory = InventoryFilter.EXPIRING_SOON)), "soon")
    @Test fun `expired filter keeps expired products`() = assertIds(listOf(product("fresh"), product("old", expirationStatus = ExpirationStatus.EXPIRED)).applyFilters(ProductListFilters(inventory = InventoryFilter.EXPIRED)), "old")
    @Test fun `category filter matches explicit category id`() = assertIds(listOf(product("milk", categoryId = "dairy"), product("rice", categoryId = "cereals")).applyFilters(ProductListFilters(categoryId = "dairy")), "milk")
    @Test fun `category filter matches system dairy id`() = assertIds(listOf(product("milk", category = ProductCategory.DAIRY, categoryId = null), product("rice", category = ProductCategory.CEREALS, categoryId = null)).applyFilters(ProductListFilters(categoryId = ProductCategoryOption.DAIRY_SYSTEM_ID)), "milk")
    @Test fun `category and low stock filters compose`() = assertIds(listOf(product("milk", categoryId = "dairy", remainingAmount = 1.0, lowStockThreshold = 2.0), product("rice", categoryId = "dairy", remainingAmount = 5.0, lowStockThreshold = 2.0)).applyFilters(ProductListFilters(categoryId = "dairy", inventory = InventoryFilter.LOW_STOCK)), "milk")
    @Test fun `low stock products sort first`() = assertEquals("low", listOf(product("normal", remainingAmount = 10.0, lowStockThreshold = 1.0), product("low", remainingAmount = 0.5, lowStockThreshold = 1.0)).applyFilters(ProductListFilters()).first().id)
    @Test fun `products sort by expiration date after low stock`() = assertIds(listOf(product("later", expirationDate = LocalDate.parse("2026-06-20")), product("soon", expirationDate = LocalDate.parse("2026-06-10"))).applyFilters(ProductListFilters()), "soon", "later")
    @Test fun `products sort by name after expiration`() = assertIds(listOf(product("b", name = "B"), product("a", name = "A")).applyFilters(ProductListFilters()), "a", "b")

    @Test fun `load style low stock filter handles five thousand products`() = assertEquals(1_000, (1..5_000).map { product("p$it", remainingAmount = if (it % 5 == 0) 1.0 else 10.0, lowStockThreshold = 2.0) }.applyFilters(ProductListFilters(inventory = InventoryFilter.LOW_STOCK)).size)
    @Test fun `load style expiration filter handles four thousand products`() = assertEquals(1_000, (1..4_000).map { product("p$it", expirationStatus = if (it % 4 == 0) ExpirationStatus.EXPIRED else ExpirationStatus.FRESH) }.applyFilters(ProductListFilters(inventory = InventoryFilter.EXPIRED)).size)
    @Test fun `load style category filter handles three thousand products`() = assertEquals(1_500, (1..3_000).map { product("p$it", category = if (it % 2 == 0) ProductCategory.DAIRY else ProductCategory.CEREALS, categoryId = null) }.applyFilters(ProductListFilters(categoryId = ProductCategoryOption.DAIRY_SYSTEM_ID)).size)

    @Test fun `expiration reminder is planned by default`() = assertReminderKinds(product(expirationDate = LocalDate.parse("2026-06-10")), ProductLocalReminderKind.EXPIRATION)
    @Test fun `low stock reminder is planned by default`() = assertReminderKinds(product(remainingAmount = 1.0, lowStockThreshold = 2.0), ProductLocalReminderKind.LOW_STOCK)
    @Test fun `both reminders can be planned for one product`() = assertEquals(2, plan(product(expirationDate = LocalDate.parse("2026-06-10"), remainingAmount = 1.0, lowStockThreshold = 2.0)).size)
    @Test fun `expiration reminder is skipped when disabled`() = assertTrue(plan(product(expirationDate = LocalDate.parse("2026-06-10")), NotificationSettings(expirationRemindersEnabled = false)).isEmpty())
    @Test fun `low stock reminder is skipped when disabled`() = assertTrue(plan(product(remainingAmount = 1.0, lowStockThreshold = 2.0), NotificationSettings(lowStockRemindersEnabled = false)).isEmpty())
    @Test fun `expiration reminder trigger subtracts configured days`() = assertEquals("2026-06-07", plan(product(expirationDate = LocalDate.parse("2026-06-10")), NotificationSettings(expirationReminderDays = 3)).single().triggerDateIso)
    @Test fun `expiration reminder id contains product id`() = assertTrue(plan(product("milk", expirationDate = LocalDate.parse("2026-06-10"))).single().id.contains("milk"))
    @Test fun `low stock reminder has no trigger date`() = assertNull(plan(product(remainingAmount = 1.0, lowStockThreshold = 2.0)).single().triggerDateIso)
    @Test fun `low stock grams message uses Russian grams`() = assertTrue(plan(product(quantityUnit = QuantityUnit.GRAMS, remainingAmount = 1.0, lowStockThreshold = 2.0)).single().message.contains(" г"))
    @Test fun `low stock milliliters message uses Russian milliliters`() = assertTrue(plan(product(quantityUnit = QuantityUnit.MILLILITERS, remainingAmount = 1.0, lowStockThreshold = 2.0)).single().message.contains(" мл"))
    @Test fun `low stock pieces message uses Russian pieces`() = assertTrue(plan(product(quantityUnit = QuantityUnit.PIECES, remainingAmount = 1.0, lowStockThreshold = 2.0)).single().message.contains(" шт."))
    @Test fun `planner skips product without expiration and threshold`() = assertTrue(plan(product()).isEmpty())
    @Test fun `planner handles large product list`() = assertEquals(1_000, ProductLocalReminderPlanner().plan((1..1_000).map { product("p$it", remainingAmount = 1.0, lowStockThreshold = 2.0) }, NotificationSettings()).size)

    @Test fun `quantity unit grams enum name stays stable`() = assertEquals("GRAMS", QuantityUnit.GRAMS.name)
    @Test fun `quantity unit milliliters enum name stays stable`() = assertEquals("MILLILITERS", QuantityUnit.MILLILITERS.name)
    @Test fun `quantity unit pieces enum name stays stable`() = assertEquals("PIECES", QuantityUnit.PIECES.name)
    @Test fun `expiration fresh enum name stays stable`() = assertEquals("FRESH", ExpirationStatus.FRESH.name)
    @Test fun `expiration expiring soon enum name stays stable`() = assertEquals("EXPIRING_SOON", ExpirationStatus.EXPIRING_SOON.name)
    @Test fun `expiration expired enum name stays stable`() = assertEquals("EXPIRED", ExpirationStatus.EXPIRED.name)
    @Test fun `expiration unknown enum name stays stable`() = assertEquals("UNKNOWN", ExpirationStatus.UNKNOWN.name)
    @Test fun `product category dairy enum name stays stable`() = assertEquals("DAIRY", ProductCategory.DAIRY.name)
    @Test fun `product category meat fish enum name stays stable`() = assertEquals("MEAT_FISH", ProductCategory.MEAT_FISH.name)
    @Test fun `product category vegetables fruits enum name stays stable`() = assertEquals("VEGETABLES_FRUITS", ProductCategory.VEGETABLES_FRUITS.name)
    @Test fun `product category cereals enum name stays stable`() = assertEquals("CEREALS", ProductCategory.CEREALS.name)
    @Test fun `product category beverages enum name stays stable`() = assertEquals("BEVERAGES", ProductCategory.BEVERAGES.name)
    @Test fun `product category other enum name stays stable`() = assertEquals("OTHER", ProductCategory.OTHER.name)
    @Test fun `notification default expiration days stays stable`() = assertEquals(3, NotificationSettings.DEFAULT_EXPIRATION_REMINDER_DAYS)
    @Test fun `notification minimum expiration days stays stable`() = assertEquals(1, NotificationSettings.MIN_EXPIRATION_REMINDER_DAYS)
    @Test fun `notification maximum expiration days stays stable`() = assertEquals(30, NotificationSettings.MAX_EXPIRATION_REMINDER_DAYS)
    @Test fun `default product remaining amount equals quantity`() = assertEquals(10.0, product(quantity = 10.0).remainingAmount)
    @Test fun `product keeps package amount metadata`() = assertEquals(950.0, product(packageAmount = 950.0).packageAmount)
    @Test fun `product keeps nutrition metadata`() = assertEquals(60.0, product(calories = 60.0).calories)

    private fun assertSystem(category: ProductCategory, id: String) =
        assertEquals(id, ProductCategoryOption.systemDefaults().single { it.code == category }.id)

    private fun assertIds(products: List<Product>, vararg ids: String) =
        assertEquals(ids.toList(), products.map { it.id })

    private fun assertReminderKinds(product: Product, vararg kinds: ProductLocalReminderKind) =
        assertEquals(kinds.toList(), plan(product).map { it.kind })

    private fun plan(product: Product, settings: NotificationSettings = NotificationSettings()) =
        ProductLocalReminderPlanner().plan(listOf(product), settings)

    private fun products(vararg ids: String): List<Product> = ids.map(::product)

    private fun categoryOption(code: ProductCategory?) =
        ProductCategoryOption(id = "category-id", code = code, name = "Category", system = code != null, createdAt = "2026-01-01T00:00:00Z")

    private fun product(
        id: String = "product-id",
        name: String = id,
        category: ProductCategory = ProductCategory.OTHER,
        categoryId: String? = category.name.lowercase(),
        categoryName: String? = null,
        quantity: Double = 10.0,
        quantityUnit: QuantityUnit = QuantityUnit.PIECES,
        packageAmount: Double? = null,
        imageUrl: String? = null,
        localImagePath: String? = null,
        calories: Double? = null,
        remainingAmount: Double = quantity,
        lowStockThreshold: Double? = null,
        expirationDate: LocalDate? = null,
        expirationStatus: ExpirationStatus = ExpirationStatus.FRESH
    ) = Product(
        id = id,
        name = name,
        category = category,
        categoryId = categoryId,
        categoryName = categoryName,
        quantity = quantity,
        quantityUnit = quantityUnit,
        packageAmount = packageAmount,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        calories = calories,
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        expirationDate = expirationDate,
        expirationStatus = expirationStatus,
        householdId = "household-id",
        addedByUserId = "user-id",
        createdAt = "2026-06-01T00:00:00Z"
    )
}
