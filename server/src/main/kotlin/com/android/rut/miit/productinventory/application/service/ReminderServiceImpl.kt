package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.model.NotificationType
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ReminderRunResult
import com.android.rut.miit.productinventory.domain.port.inbound.IReminderService
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSettingsRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ReminderServiceImpl(
    private val productRepository: IProductRepository,
    private val membershipRepository: IMembershipRepository,
    private val notificationRepository: INotificationRepository,
    private val notificationSettingsRepository: INotificationSettingsRepository,
    private val notificationSender: INotificationSender
) : IReminderService {

    @Transactional
    override fun runDueReminders(referenceDate: LocalDate): ReminderRunResult {
        val products = DueReminderProducts(
            expiring = productRepository.findExpiringBetween(
                startInclusive = referenceDate,
                endExclusive = referenceDate.plusDays(MAX_EXPIRATION_REMINDER_DAYS + 1)
            ),
            lowStock = productRepository.findLowStock()
        )
        return notifyProducts(products, referenceDate)
    }

    @Transactional
    override fun runHouseholdDueReminders(
        userId: UUID,
        householdId: UUID,
        referenceDate: LocalDate
    ): ReminderRunResult {
        requireMembership(userId, householdId)
        val products = DueReminderProducts(
            expiring = productRepository.findExpiringBetweenByHouseholdId(
                householdId = householdId,
                startInclusive = referenceDate,
                endExclusive = referenceDate.plusDays(MAX_EXPIRATION_REMINDER_DAYS + 1)
            ),
            lowStock = productRepository.findLowStockByHouseholdId(householdId)
        )
        return notifyProducts(products, referenceDate)
    }

    private fun notifyProducts(products: DueReminderProducts, referenceDate: LocalDate): ReminderRunResult {
        val expiringCreated = products.expiring.sumOf { product ->
            notifyHouseholdMembers(
                product = product,
                type = NotificationType.REMINDER_EXPIRING_SOON,
                title = "Product expires soon",
                message = "${product.name} expires on ${product.expirationDate?.date}",
                dedupeKey = "reminder:expiring:${product.id}:${product.expirationDate?.date}",
                referenceDate = referenceDate
            )
        }
        val lowStockCreated = products.lowStock.sumOf { product ->
            notifyHouseholdMembers(
                product = product,
                type = NotificationType.REMINDER_LOW_STOCK,
                title = "Low stock",
                message = "${product.name} has ${product.remainingAmount} ${product.quantity.unit.name.lowercase()} left",
                dedupeKey = "reminder:low-stock:${product.id}",
                referenceDate = referenceDate
            )
        }
        return ReminderRunResult(
            expiringProducts = products.expiring.size,
            lowStockProducts = products.lowStock.size,
            notificationsCreated = expiringCreated + lowStockCreated
        )
    }

    private fun notifyHouseholdMembers(
        product: Product,
        type: NotificationType,
        title: String,
        message: String,
        dedupeKey: String,
        referenceDate: LocalDate
    ): Int =
        membershipRepository.findByHouseholdId(product.householdId).count { membership ->
            createNotificationIfAbsent(
                userId = membership.userId,
                product = product,
                type = type,
                title = title,
                message = message,
                dedupeKey = dedupeKey,
                referenceDate = referenceDate
            )
        }

    private fun createNotificationIfAbsent(
        userId: UUID,
        product: Product,
        type: NotificationType,
        title: String,
        message: String,
        dedupeKey: String,
        referenceDate: LocalDate
    ): Boolean {
        val settings = notificationSettingsRepository.findByUserId(userId)
            ?: NotificationSettings(userId = userId)
        if (!settings.allows(type, product, referenceDate)) return false
        if (notificationRepository.existsByUserIdAndDedupeKey(userId, dedupeKey)) return false

        val saved = runCatching {
            notificationRepository.save(
                Notification(
                    userId = userId,
                    type = type,
                    title = title,
                    message = message,
                    householdId = product.householdId,
                    productId = product.id,
                    dedupeKey = dedupeKey
                )
            )
        }.getOrElse { error ->
            if (error is DataIntegrityViolationException) return false
            throw error
        }

        if (settings.pushEnabled) {
            notificationSender.sendPush(saved.userId, saved.title, saved.message)
        }
        return true
    }

    private fun requireMembership(userId: UUID, householdId: UUID) {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")
    }

    private data class DueReminderProducts(
        val expiring: List<Product>,
        val lowStock: List<Product>
    )

    private companion object {
        const val MAX_EXPIRATION_REMINDER_DAYS = NotificationSettings.DEFAULT_EXPIRATION_REMINDER_DAYS * 10L
    }
}

private fun NotificationSettings.allows(
    type: NotificationType,
    product: Product,
    referenceDate: LocalDate
): Boolean =
    when (type) {
        NotificationType.REMINDER_EXPIRING_SOON ->
            expirationRemindersEnabled &&
                product.expirationDate?.date?.isAfter(referenceDate.plusDays(expirationReminderDays.toLong())) != true
        NotificationType.REMINDER_LOW_STOCK -> lowStockRemindersEnabled
        NotificationType.GENERAL -> true
    }
