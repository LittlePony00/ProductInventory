package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ProductServiceImpl(
    private val productRepository: IProductRepository,
    private val membershipRepository: IMembershipRepository,
    private val notificationRepository: INotificationRepository,
    private val notificationSender: INotificationSender
) : IProductService {

    @Transactional
    override fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        barcode: String?,
        brand: String?,
        ingredients: String?,
        caloriesKcal: Double?,
        proteinGrams: Double?,
        fatGrams: Double?,
        carbohydratesGrams: Double?,
        category: ProductCategory,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?
    ): Product {
        requireMembership(userId, householdId)

        val product = productRepository.save(
            Product(
                name = name.trim(),
                barcode = barcode?.trim()?.takeIf { it.isNotBlank() },
                brand = brand?.trim()?.takeIf { it.isNotBlank() },
                ingredients = ingredients?.trim()?.takeIf { it.isNotBlank() },
                caloriesKcal = caloriesKcal,
                proteinGrams = proteinGrams,
                fatGrams = fatGrams,
                carbohydratesGrams = carbohydratesGrams,
                category = category,
                quantity = Quantity(value = quantity, unit = quantityUnit),
                expirationDate = expirationDate?.let { ExpirationDate(it) },
                householdId = householdId,
                addedByUserId = userId
            )
        )

        notifyOtherMembers(userId, householdId, "New product", "${product.name} was added")

        return product
    }

    @Transactional
    override fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        barcode: String?,
        brand: String?,
        ingredients: String?,
        caloriesKcal: Double?,
        proteinGrams: Double?,
        fatGrams: Double?,
        carbohydratesGrams: Double?,
        category: ProductCategory?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?
    ): Product {
        val existing = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, existing.householdId)

        val updated = existing.copy(
            name = name?.trim() ?: existing.name,
            barcode = barcode?.trim()?.takeIf { it.isNotBlank() } ?: existing.barcode,
            brand = brand?.trim()?.takeIf { it.isNotBlank() } ?: existing.brand,
            ingredients = ingredients?.trim()?.takeIf { it.isNotBlank() } ?: existing.ingredients,
            caloriesKcal = caloriesKcal ?: existing.caloriesKcal,
            proteinGrams = proteinGrams ?: existing.proteinGrams,
            fatGrams = fatGrams ?: existing.fatGrams,
            carbohydratesGrams = carbohydratesGrams ?: existing.carbohydratesGrams,
            category = category ?: existing.category,
            quantity = Quantity(
                value = quantity ?: existing.quantity.value,
                unit = quantityUnit ?: existing.quantity.unit
            ),
            expirationDate = expirationDate?.let { ExpirationDate(it) } ?: existing.expirationDate
        )

        return productRepository.save(updated)
    }

    @Transactional
    override fun deleteProduct(userId: UUID, productId: UUID) {
        val product = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, product.householdId)
        productRepository.deleteById(productId)
    }

    @Transactional(readOnly = true)
    override fun getProducts(userId: UUID, householdId: UUID): List<Product> {
        requireMembership(userId, householdId)
        return productRepository.findByHouseholdId(householdId)
    }

    @Transactional(readOnly = true)
    override fun getProduct(userId: UUID, productId: UUID): Product {
        val product = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, product.householdId)
        return product
    }

    @Transactional(readOnly = true)
    override fun getExpiringProducts(userId: UUID, householdId: UUID, days: Int): List<Product> {
        requireMembership(userId, householdId)
        return productRepository.findExpiringBefore(householdId, LocalDate.now().plusDays(days.toLong()))
    }

    private fun requireMembership(userId: UUID, householdId: UUID) {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")
    }

    private fun notifyOtherMembers(actorId: UUID, householdId: UUID, title: String, message: String) {
        val members = membershipRepository.findByHouseholdId(householdId)
        members.filter { it.userId != actorId }.forEach { membership ->
            notificationRepository.save(
                Notification(userId = membership.userId, title = title, message = message)
            )
            notificationSender.sendPush(membership.userId, title, message)
        }
    }
}
