package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable

@Serializable
data class ProductLocalReminder(
    val id: String,
    val kind: ProductLocalReminderKind,
    val title: String,
    val message: String,
    val productId: String,
    val householdId: String,
    val triggerDateIso: String?
)

enum class ProductLocalReminderKind {
    EXPIRATION,
    LOW_STOCK
}

class ProductLocalReminderPlanner {
    fun plan(products: List<Product>, settings: NotificationSettings): List<ProductLocalReminder> =
        products.flatMap { product ->
            buildList {
                expirationReminder(product, settings)?.let(::add)
                lowStockReminder(product, settings)?.let(::add)
            }
        }

    private fun expirationReminder(product: Product, settings: NotificationSettings): ProductLocalReminder? {
        val expirationDate = product.expirationDate ?: return null
        if (!settings.expirationRemindersEnabled) return null
        val triggerDate = expirationDate.minus(DatePeriod(days = settings.expirationReminderDays))
        return ProductLocalReminder(
            id = "expiration:${product.householdId}:${product.id}:$expirationDate:${settings.expirationReminderDays}",
            kind = ProductLocalReminderKind.EXPIRATION,
            title = "Скоро истекает срок годности",
            message = "Срок годности продукта «${product.name}» истекает $expirationDate",
            productId = product.id,
            householdId = product.householdId,
            triggerDateIso = triggerDate.toString()
        )
    }

    private fun lowStockReminder(product: Product, settings: NotificationSettings): ProductLocalReminder? {
        val threshold = product.lowStockThreshold ?: return null
        if (!settings.lowStockRemindersEnabled || product.remainingAmount > threshold) return null
        return ProductLocalReminder(
            id = "low-stock:${product.householdId}:${product.id}:${product.remainingAmount}:$threshold",
            kind = ProductLocalReminderKind.LOW_STOCK,
            title = "Продукт заканчивается",
            message = "Осталось: «${product.name}» — ${product.remainingAmount} ${product.quantityUnit.displayNameRu()}",
            productId = product.id,
            householdId = product.householdId,
            triggerDateIso = null
        )
    }
}

private fun QuantityUnit.displayNameRu(): String =
    when (this) {
        QuantityUnit.GRAMS -> "г"
        QuantityUnit.MILLILITERS -> "мл"
        QuantityUnit.PIECES -> "шт."
    }
