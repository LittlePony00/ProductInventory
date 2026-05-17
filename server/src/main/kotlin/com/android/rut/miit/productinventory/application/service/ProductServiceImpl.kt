package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ProductServiceImpl(
    private val productRepository: IProductRepository,
    private val membershipRepository: IMembershipRepository,
    private val notificationRepository: INotificationRepository,
    private val notificationSender: INotificationSender,
    private val householdEventPublisher: IHouseholdEventPublisher,
    private val categoryRepository: ICategoryRepository,
    private val barcodeProductCacheRepository: IBarcodeProductCacheRepository
) : IProductService {

    @Transactional
    override fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        brand: String?,
        barcode: String?,
        category: ProductCategory,
        categoryId: UUID?,
        quantity: Double,
        quantityUnit: QuantityUnit,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product {
        requireMembership(userId, householdId)
        val resolvedCategory = resolveCategory(householdId, category, categoryId)

        val product = productRepository.save(
            Product(
                name = name.trim(),
                brand = brand?.trimToNull(),
                barcode = barcode?.trimToNull(),
                category = resolvedCategory.legacyCategory,
                categoryId = resolvedCategory.id,
                categoryName = resolvedCategory.name,
                quantity = Quantity(value = quantity, unit = quantityUnit),
                packageQuantity = packageAmount?.let { Quantity(value = it, unit = packageUnit ?: quantityUnit) },
                ingredientsText = ingredientsText?.trimToNull(),
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                purchaseDate = purchaseDate,
                remainingAmount = remainingAmount ?: quantity,
                lowStockThreshold = lowStockThreshold,
                expirationDate = expirationDate?.let { ExpirationDate(it) },
                householdId = householdId,
                addedByUserId = userId
            )
        )

        publishProductEvent(HouseholdEventType.PRODUCT_CREATED, userId, product)
        publishStateEvents(userId, product)
        notifyOtherMembers(userId, householdId, "New product", "${product.name} was added")
        saveBarcodeMetadata(product)

        return product.withCategoryDetails(householdId)
    }

    @Transactional
    override fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        brand: String?,
        barcode: String?,
        category: ProductCategory?,
        categoryId: UUID?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product {
        val existing = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, existing.householdId)
        val resolvedCategory = when {
            categoryId != null -> resolveCategory(existing.householdId, category ?: existing.category, categoryId)
            category != null -> resolveCategory(existing.householdId, category, null)
            else -> null
        }

        val updatedQuantityUnit = quantityUnit ?: existing.quantity.unit
        val updated = existing.copy(
            name = name?.trim() ?: existing.name,
            brand = brand?.trimToNull() ?: existing.brand,
            barcode = barcode?.trimToNull() ?: existing.barcode,
            category = resolvedCategory?.legacyCategory ?: category ?: existing.category,
            categoryId = resolvedCategory?.id ?: existing.categoryId,
            categoryName = resolvedCategory?.name ?: existing.categoryName,
            quantity = Quantity(
                value = quantity ?: existing.quantity.value,
                unit = updatedQuantityUnit
            ),
            packageQuantity = when {
                packageAmount != null -> Quantity(value = packageAmount, unit = packageUnit ?: updatedQuantityUnit)
                packageUnit != null && existing.packageQuantity != null -> existing.packageQuantity.copy(unit = packageUnit)
                else -> existing.packageQuantity
            },
            ingredientsText = ingredientsText?.trimToNull() ?: existing.ingredientsText,
            calories = calories ?: existing.calories,
            protein = protein ?: existing.protein,
            fat = fat ?: existing.fat,
            carbs = carbs ?: existing.carbs,
            purchaseDate = purchaseDate ?: existing.purchaseDate,
            remainingAmount = remainingAmount ?: existing.remainingAmount,
            lowStockThreshold = lowStockThreshold ?: existing.lowStockThreshold,
            expirationDate = expirationDate?.let { ExpirationDate(it) } ?: existing.expirationDate
        )

        val saved = productRepository.save(updated)

        publishProductEvent(HouseholdEventType.PRODUCT_UPDATED, userId, saved)
        if (saved.category != existing.category) {
            householdEventPublisher.publish(
                HouseholdEvent(
                    type = HouseholdEventType.CATEGORY_CHANGED,
                    householdId = saved.householdId,
                    actorUserId = userId,
                    productId = saved.id,
                    productName = saved.name,
                    previousCategory = existing.category,
                    currentCategory = saved.category
                )
            )
        }
        publishStateTransitionEvents(userId, existing, saved)
        saveBarcodeMetadata(saved)

        return saved.withCategoryDetails(saved.householdId)
    }

    @Transactional
    override fun consumeProduct(userId: UUID, productId: UUID, amount: Double): Product {
        require(amount > 0) { "Consumption amount must be positive" }
        val existing = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, existing.householdId)
        require(amount <= existing.remainingAmount) {
            "Consumption amount cannot exceed remaining amount"
        }

        val saved = productRepository.save(
            existing.copy(remainingAmount = existing.remainingAmount - amount)
        )

        publishProductEvent(HouseholdEventType.PRODUCT_QUANTITY_CHANGED, userId, saved)
        if (existing.remainingAmount > 0.0 && saved.remainingAmount == 0.0) {
            publishProductEvent(HouseholdEventType.PRODUCT_DEPLETED, userId, saved)
        }
        publishStateTransitionEvents(userId, existing, saved)

        return saved.withCategoryDetails(saved.householdId)
    }

    @Transactional
    override fun deleteProduct(userId: UUID, productId: UUID) {
        val product = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, product.householdId)
        productRepository.deleteById(productId)
        publishProductEvent(HouseholdEventType.PRODUCT_DELETED, userId, product)
    }

    @Transactional(readOnly = true)
    override fun getProducts(userId: UUID, householdId: UUID, categoryId: UUID?): List<Product> {
        requireMembership(userId, householdId)
        return (categoryId
            ?.let { productRepository.findByHouseholdIdAndCategoryId(householdId, it) }
            ?: productRepository.findByHouseholdId(householdId))
            .map { it.withCategoryDetails(householdId) }
    }

    @Transactional(readOnly = true)
    override fun getProduct(userId: UUID, productId: UUID): Product {
        val product = productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product", productId)

        requireMembership(userId, product.householdId)
        return product.withCategoryDetails(product.householdId)
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

    private fun resolveCategory(
        householdId: UUID,
        fallbackCategory: ProductCategory,
        categoryId: UUID?
    ): ResolvedCategory {
        val category = categoryId?.let {
            categoryRepository.findAvailableById(it, householdId)
                ?: throw EntityNotFoundException("Category", it)
        }
        return ResolvedCategory(
            id = category?.id ?: SystemCategoryCatalog.idFor(fallbackCategory),
            legacyCategory = category?.code ?: fallbackCategory,
            name = category?.name ?: categoryRepository.findAvailableById(
                SystemCategoryCatalog.idFor(fallbackCategory),
                householdId
            )?.name
        )
    }

    private fun Product.withCategoryDetails(householdId: UUID): Product {
        val resolvedId = categoryId ?: SystemCategoryCatalog.idFor(category)
        val resolvedName = categoryName ?: categoryRepository.findAvailableById(resolvedId, householdId)?.name
        return copy(categoryId = resolvedId, categoryName = resolvedName)
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

    private fun publishProductEvent(type: HouseholdEventType, actorId: UUID, product: Product) {
        householdEventPublisher.publish(
            HouseholdEvent(
                type = type,
                householdId = product.householdId,
                actorUserId = actorId,
                productId = product.id,
                productName = product.name,
                currentCategory = product.category
            )
        )
    }

    private fun publishStateEvents(actorId: UUID, product: Product) {
        if (product.isInventoryLow()) {
            publishProductEvent(HouseholdEventType.INVENTORY_LOW, actorId, product)
        }
        if (product.isExpiringSoon()) {
            publishProductEvent(HouseholdEventType.EXPIRING_SOON, actorId, product)
        }
    }

    private fun publishStateTransitionEvents(actorId: UUID, previous: Product, current: Product) {
        if (!previous.isInventoryLow() && current.isInventoryLow()) {
            publishProductEvent(HouseholdEventType.INVENTORY_LOW, actorId, current)
        }
        if (!previous.isExpiringSoon() && current.isExpiringSoon()) {
            publishProductEvent(HouseholdEventType.EXPIRING_SOON, actorId, current)
        }
    }

    private fun Product.isInventoryLow(): Boolean =
        lowStockThreshold?.let { remainingAmount <= it } ?: false

    private fun Product.isExpiringSoon(): Boolean =
        expirationDate?.status == ExpirationStatus.EXPIRING_SOON

    private fun saveBarcodeMetadata(product: Product) {
        val barcode = product.barcode?.trimToNull() ?: return
        val name = product.name.trimToNull() ?: return

        barcodeProductCacheRepository.save(
            BarcodeProductDraft(
                barcode = barcode,
                name = name,
                brand = product.brand,
                packageQuantity = product.packageQuantity ?: product.quantity,
                ingredients = product.ingredientsText,
                nutrition = NutritionFacts(
                    caloriesKcal = product.calories,
                    proteinGrams = product.protein,
                    fatGrams = product.fat,
                    carbohydratesGrams = product.carbs
                ),
                category = product.category,
                source = BarcodeProductSource.LOCAL_DATABASE,
                confidence = LOCAL_DATABASE_CACHE_CONFIDENCE
            )
        )
    }

    private companion object {
        const val LOCAL_DATABASE_CACHE_CONFIDENCE = 0.95
    }
}

private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }

private data class ResolvedCategory(
    val id: UUID,
    val legacyCategory: ProductCategory,
    val name: String?
)
