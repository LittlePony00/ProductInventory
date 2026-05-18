package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.CreateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.ConsumeProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingCreateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUpdateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateProductRequestDto
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource,
    private val localDataSource: ProductLocalDataSource,
    private val syncQueue: SyncQueue,
    private val imageFileReader: ProductImageFileReader = NoopProductImageFileReader,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ProductRepository {
    private var lastActionCreatedAt = 0L
    private val syncMutex = Mutex()

    override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> {
        runCatching { syncPendingActions(householdId) }
        return try {
            val remote = remoteDataSource.getProducts(householdId, categoryId).map { it.toDomain() }
            if (categoryId == null) {
                val withLocalImages = preserveLocalImagePaths(householdId, remote)
                val merged = mergeRemoteWithPendingLocal(householdId, withLocalImages)
                localDataSource.saveProducts(householdId, merged)
                merged
            } else {
                remote
            }
        } catch (e: Exception) {
            val local = localDataSource.getProducts(householdId)
            val filtered = categoryId?.let { id -> local.filter { it.categoryId == id } } ?: local
            if (filtered.isNotEmpty()) filtered else throw e
        }
    }

    override suspend fun getProduct(householdId: String, productId: String): Product {
        return try {
            val remote = remoteDataSource.getProduct(householdId, productId).toDomain()
            val localImagePath = localDataSource.getProduct(householdId, productId)?.localImagePath
            val product = remote.copy(localImagePath = localImagePath)
            localDataSource.saveProduct(product)
            product
        } catch (e: Exception) {
            localDataSource.getProduct(householdId, productId) ?: throw e
        }
    }

    override suspend fun addProduct(
        householdId: String,
        name: String,
        category: ProductCategory,
        categoryId: String?,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?,
        brand: String?,
        barcode: String?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        imageUrl: String?,
        localImagePath: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?
    ): Product {
        val request = CreateProductRequestDto(
            name = name,
            brand = brand,
            barcode = barcode,
            category = category.name,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit.name,
            packageAmount = packageAmount,
            packageUnit = packageUnit?.name,
            ingredientsText = ingredientsText,
            imageUrl = imageUrl,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate?.toString(),
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.toString()
        )
        runCatching { syncPendingActions(householdId) }
        return try {
            val created = remoteDataSource.addProduct(householdId, request).toDomain()
            val product = runCatching {
                uploadLocalImageIfNeeded(householdId, created.id, localImagePath, created)
            }.getOrElse {
                if (!localImagePath.isNullOrBlank()) {
                    queueAction(
                        type = SyncActionType.UPDATE_PRODUCT,
                        entityId = created.id,
                        householdId = householdId,
                        payload = json.encodeToString(PendingUpdateProductPayloadDto(UpdateProductRequestDto(), localImagePath))
                    )
                }
                created.copy(localImagePath = localImagePath)
            }
            localDataSource.saveProduct(product)
            product
        } catch (e: Exception) {
            val localProduct = request.toLocalProduct(
                householdId = householdId,
                id = generateUuid(),
                localImagePath = localImagePath
            )
            localDataSource.saveProduct(localProduct)
            queueAction(
                type = SyncActionType.ADD_PRODUCT,
                entityId = localProduct.id,
                householdId = householdId,
                payload = json.encodeToString(PendingCreateProductPayloadDto(request, localImagePath))
            )
            localProduct
        }
    }

    override suspend fun updateProduct(
        householdId: String,
        productId: String,
        name: String?,
        category: ProductCategory?,
        categoryId: String?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?,
        brand: String?,
        barcode: String?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        imageUrl: String?,
        localImagePath: String?,
        clearImage: Boolean,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?
    ): Product {
        val request = UpdateProductRequestDto(
            name = name,
            brand = brand,
            barcode = barcode,
            category = category?.name,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit?.name,
            packageAmount = packageAmount,
            packageUnit = packageUnit?.name,
            ingredientsText = ingredientsText,
            imageUrl = imageUrl,
            clearImage = clearImage,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate?.toString(),
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.toString()
        )
        runCatching { syncPendingActions(householdId) }
        if (hasPendingActionForProduct(productId)) {
            return updateLocalAndQueue(householdId, productId, request, localImagePath)
        }
        return try {
            val updated = remoteDataSource.updateProduct(householdId, productId, request).toDomain()
            val product = uploadLocalImageIfNeeded(householdId, productId, localImagePath, updated)
            localDataSource.saveProduct(product)
            product
        } catch (e: Exception) {
            updateLocalAndQueue(householdId, productId, request, localImagePath)
        }
    }

    override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product {
        val request = ConsumeProductRequestDto(amount = amount)
        runCatching { syncPendingActions(householdId) }
        if (hasPendingActionForProduct(productId)) {
            return consumeLocalAndQueue(householdId, productId, request)
        }
        return try {
            val product = remoteDataSource.consumeProduct(
                householdId = householdId,
                productId = productId,
                request = request
            ).toDomain()
            localDataSource.saveProduct(product)
            product
        } catch (e: Exception) {
            consumeLocalAndQueue(householdId, productId, request)
        }
    }

    override suspend fun deleteProduct(householdId: String, productId: String) {
        runCatching { syncPendingActions(householdId) }
        if (hasPendingAdd(productId)) {
            syncQueue.getPendingActions()
                .filter { it.householdId == householdId && it.entityId == productId }
                .forEach { syncQueue.removePendingAction(it.id) }
            localDataSource.deleteProduct(productId)
            return
        }
        try {
            remoteDataSource.deleteProduct(householdId, productId)
            localDataSource.deleteProduct(productId)
        } catch (e: Exception) {
            localDataSource.deleteProduct(productId)
            queueAction(
                type = SyncActionType.DELETE_PRODUCT,
                entityId = productId,
                householdId = householdId,
                payload = "{}"
            )
        }
    }

    override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> {
        return try {
            remoteDataSource.getExpiringProducts(householdId, days).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun suggestProductEnrichment(
        householdId: String,
        name: String?,
        brand: String?,
        barcode: String?,
        ingredientsText: String?
    ): ProductEnrichmentSuggestion =
        remoteDataSource.suggestProductEnrichment(
            householdId = householdId,
            request = ProductEnrichmentSuggestionRequestDto(
                name = name,
                brand = brand,
                barcode = barcode,
                ingredientsText = ingredientsText
            )
        ).toDomain()

    override suspend fun upsertCachedProduct(product: Product) {
        localDataSource.saveProduct(product)
    }

    override suspend fun deleteCachedProduct(productId: String) {
        localDataSource.deleteProduct(productId)
    }

    private suspend fun updateLocalAndQueue(
        householdId: String,
        productId: String,
        request: UpdateProductRequestDto,
        localImagePath: String? = null
    ): Product {
        val current = localDataSource.getProduct(householdId, productId)
            ?: throw IllegalStateException("Product is not cached for offline update")
        val updated = current.applyUpdate(request, localImagePath)
        localDataSource.saveProduct(updated)
        queueAction(
            type = SyncActionType.UPDATE_PRODUCT,
            entityId = productId,
            householdId = householdId,
            payload = json.encodeToString(PendingUpdateProductPayloadDto(request, localImagePath))
        )
        return updated
    }

    private suspend fun consumeLocalAndQueue(
        householdId: String,
        productId: String,
        request: ConsumeProductRequestDto
    ): Product {
        val current = localDataSource.getProduct(householdId, productId)
            ?: throw IllegalStateException("Product is not cached for offline consume")
        val updated = current.copy(remainingAmount = (current.remainingAmount - request.amount).coerceAtLeast(0.0))
        localDataSource.saveProduct(updated)
        queueAction(
            type = SyncActionType.CONSUME_PRODUCT,
            entityId = productId,
            householdId = householdId,
            payload = json.encodeToString(request)
        )
        return updated
    }

    private suspend fun syncPendingActions(householdId: String) {
        syncMutex.withLock {
            val idMappings = mutableMapOf<String, String>()
            syncQueue.getPendingActions()
                .filter { it.householdId == householdId }
                .sortedBy { it.createdAt }
                .forEach { action ->
                    executePendingAction(action, idMappings)
                    syncQueue.removePendingAction(action.id)
                }
        }
    }

    private suspend fun executePendingAction(action: PendingSyncAction, idMappings: MutableMap<String, String>) {
        val resolvedProductId = idMappings[action.entityId] ?: action.entityId
        when (action.type) {
            SyncActionType.ADD_PRODUCT -> {
                val payload = decodeCreatePayload(action.payload)
                val product = remoteDataSource.addProduct(
                    action.householdId,
                    payload.request
                ).toDomain()
                val uploadedProduct = uploadLocalImageIfNeeded(
                    householdId = action.householdId,
                    productId = product.id,
                    localImagePath = payload.localImagePath,
                    fallback = product
                )
                val pendingLocal = localDataSource.getProduct(action.householdId, action.entityId)
                localDataSource.saveProduct(
                    pendingLocal
                        ?.copy(
                            id = uploadedProduct.id,
                            imageUrl = uploadedProduct.imageUrl,
                            localImagePath = uploadedProduct.localImagePath,
                            addedByUserId = uploadedProduct.addedByUserId,
                            createdAt = uploadedProduct.createdAt
                        )
                        ?: uploadedProduct
                )
                if (product.id != action.entityId) {
                    localDataSource.deleteProduct(action.entityId)
                    idMappings[action.entityId] = product.id
                    remapQueuedProductId(
                        householdId = action.householdId,
                        oldProductId = action.entityId,
                        newProductId = product.id,
                        completedActionId = action.id
                    )
                }
            }
            SyncActionType.UPDATE_PRODUCT -> {
                val payload = decodeUpdatePayload(action.payload)
                val updated = remoteDataSource.updateProduct(
                    action.householdId,
                    resolvedProductId,
                    payload.request
                ).toDomain()
                val product = uploadLocalImageIfNeeded(
                    householdId = action.householdId,
                    productId = resolvedProductId,
                    localImagePath = payload.localImagePath,
                    fallback = updated
                )
                localDataSource.saveProduct(product)
            }
            SyncActionType.CONSUME_PRODUCT -> {
                val product = remoteDataSource.consumeProduct(
                    householdId = action.householdId,
                    productId = resolvedProductId,
                    request = json.decodeFromString<ConsumeProductRequestDto>(action.payload)
                ).toDomain()
                localDataSource.saveProduct(product)
            }
            SyncActionType.DELETE_PRODUCT -> {
                remoteDataSource.deleteProduct(action.householdId, resolvedProductId)
                localDataSource.deleteProduct(resolvedProductId)
            }
        }
    }

    private suspend fun remapQueuedProductId(
        householdId: String,
        oldProductId: String,
        newProductId: String,
        completedActionId: String
    ) {
        syncQueue.getPendingActions()
            .filter { action ->
                action.householdId == householdId &&
                    action.entityId == oldProductId &&
                    action.id != completedActionId
            }
            .forEach { action ->
                syncQueue.updatePendingAction(action.copy(entityId = newProductId))
            }
    }

    private suspend fun mergeRemoteWithPendingLocal(householdId: String, remote: List<Product>): List<Product> {
        val pending = syncQueue.getPendingActions().filter { it.householdId == householdId }
        if (pending.isEmpty()) return remote

        val deletedIds = pending
            .filter { it.type == SyncActionType.DELETE_PRODUCT }
            .mapTo(mutableSetOf()) { it.entityId }
        val locallyChangedIds = pending
            .filter { it.type != SyncActionType.DELETE_PRODUCT }
            .mapTo(mutableSetOf()) { it.entityId }
        val localById = localDataSource.getProducts(householdId).associateBy { it.id }
        val remoteWithoutPending = remote.filterNot { it.id in deletedIds || it.id in locallyChangedIds }
        val localPending = locallyChangedIds.mapNotNull { localById[it] }
        return remoteWithoutPending + localPending
    }

    private suspend fun preserveLocalImagePaths(householdId: String, remote: List<Product>): List<Product> {
        val localById = localDataSource.getProducts(householdId).associateBy { it.id }
        return remote.map { product ->
            product.copy(localImagePath = localById[product.id]?.localImagePath)
        }
    }

    private suspend fun hasPendingActionForProduct(productId: String): Boolean =
        syncQueue.getPendingActions().any { it.entityId == productId }

    private suspend fun hasPendingAdd(productId: String): Boolean =
        syncQueue.getPendingActions().any { it.entityId == productId && it.type == SyncActionType.ADD_PRODUCT }

    private suspend fun queueAction(
        type: SyncActionType,
        entityId: String,
        householdId: String,
        payload: String
    ) {
        val createdAt = nextActionCreatedAt()
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "${type.name}-$entityId-$createdAt-${generateUuid()}",
                type = type,
                entityId = entityId,
                householdId = householdId,
                payload = payload,
                createdAt = createdAt
            )
        )
    }

    private fun UpdateProductRequestDto.toCreateRequest(current: Product): CreateProductRequestDto =
        CreateProductRequestDto(
            name = name ?: current.name,
            brand = brand ?: current.brand,
            barcode = barcode ?: current.barcode,
            category = category ?: current.category.name,
            categoryId = categoryId ?: current.categoryId,
            quantity = quantity ?: current.quantity,
            quantityUnit = quantityUnit ?: current.quantityUnit.name,
            packageAmount = packageAmount ?: current.packageAmount,
            packageUnit = packageUnit ?: current.packageUnit?.name,
            ingredientsText = ingredientsText ?: current.ingredientsText,
            imageUrl = if (clearImage) null else imageUrl ?: current.imageUrl,
            calories = calories ?: current.calories,
            protein = protein ?: current.protein,
            fat = fat ?: current.fat,
            carbs = carbs ?: current.carbs,
            purchaseDate = purchaseDate ?: current.purchaseDate?.toString(),
            remainingAmount = remainingAmount ?: current.remainingAmount,
            lowStockThreshold = lowStockThreshold ?: current.lowStockThreshold,
            expirationDate = expirationDate ?: current.expirationDate?.toString()
        )

    private fun Product.applyUpdate(request: UpdateProductRequestDto, localImagePath: String?): Product =
        request.toCreateRequest(this).toLocalProduct(
            householdId = householdId,
            id = id,
            addedByUserId = addedByUserId,
            createdAt = createdAt,
            expirationStatus = expirationStatus,
            localImagePath = if (request.clearImage) null else localImagePath ?: this.localImagePath
        ).copy(categoryName = categoryName)

    private fun CreateProductRequestDto.toLocalProduct(
        householdId: String,
        id: String,
        addedByUserId: String = "",
        createdAt: String = nowMillis().toString(),
        expirationStatus: ExpirationStatus = ExpirationStatus.UNKNOWN,
        localImagePath: String? = null
    ): Product =
        Product(
            id = id,
            name = name,
            brand = brand,
            barcode = barcode,
            category = runCatching { ProductCategory.valueOf(category) }.getOrDefault(ProductCategory.OTHER),
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = runCatching { QuantityUnit.valueOf(quantityUnit) }.getOrDefault(QuantityUnit.PIECES),
            packageAmount = packageAmount,
            packageUnit = packageUnit?.let { runCatching { QuantityUnit.valueOf(it) }.getOrNull() },
            ingredientsText = ingredientsText,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            remainingAmount = remainingAmount ?: quantity,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            expirationStatus = expirationStatus,
            householdId = householdId,
            addedByUserId = addedByUserId,
            createdAt = createdAt
        )

    private suspend fun uploadLocalImageIfNeeded(
        householdId: String,
        productId: String,
        localImagePath: String?,
        fallback: Product
    ): Product {
        val path = localImagePath?.takeIf { it.isNotBlank() } ?: return fallback
        val image = imageFileReader.read(path) ?: return fallback.copy(localImagePath = null)
        return remoteDataSource.uploadProductImage(householdId, productId, image).toDomain()
    }

    private fun decodeCreatePayload(payload: String): PendingCreateProductPayloadDto =
        runCatching { json.decodeFromString<PendingCreateProductPayloadDto>(payload) }
            .getOrElse { PendingCreateProductPayloadDto(json.decodeFromString(payload), null) }

    private fun decodeUpdatePayload(payload: String): PendingUpdateProductPayloadDto =
        runCatching { json.decodeFromString<PendingUpdateProductPayloadDto>(payload) }
            .getOrElse { PendingUpdateProductPayloadDto(json.decodeFromString(payload), null) }

    private fun generateUuid(): String {
        val bytes = Random.nextBytes(16)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        val hex = bytes.joinToString(separator = "") { ((it.toInt() and 0xff).toString(16)).padStart(2, '0') }
        return listOf(
            hex.substring(0, 8),
            hex.substring(8, 12),
            hex.substring(12, 16),
            hex.substring(16, 20),
            hex.substring(20, 32)
        ).joinToString("-")
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun nextActionCreatedAt(): Long {
        val now = nowMillis()
        val next = maxOf(now, lastActionCreatedAt + 1)
        lastActionCreatedAt = next
        return next
    }
}
