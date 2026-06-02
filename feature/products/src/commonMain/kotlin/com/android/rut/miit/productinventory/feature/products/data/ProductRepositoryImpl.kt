package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.core.network.ApiException
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
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUploadProductImagePayloadDto
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
    private val barcodeLocalDataSource: BarcodeLocalDataSource? = null,
    private val imageFileReader: ProductImageFileReader = NoopProductImageFileReader,
    private val imageLocalCache: ProductImageLocalCache = NoopProductImageLocalCache,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ProductRepository {
    private var lastActionCreatedAt = 0L
    private val syncMutex = Mutex()

    override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> {
        val local = localDataSource.getProducts(householdId)
        return categoryId?.let { id -> local.filter { it.categoryId == id } } ?: local
    }

    override suspend fun refreshProducts(householdId: String, categoryId: String?): List<Product> {
        runCatching { syncPendingActions(householdId) }
        return try {
            val remote = remoteDataSource.getProducts(householdId, null).map { it.toDomain() }
            val withLocalImages = resolveRemoteImageCache(householdId, remote)
            val merged = mergeRemoteWithPendingLocal(householdId, withLocalImages)
            localDataSource.saveProducts(householdId, merged)
            merged.forEach { saveBarcodeCache(it) }
            categoryId?.let { id -> merged.filter { it.categoryId == id } } ?: merged
        } catch (e: Exception) {
            val local = localDataSource.getProducts(householdId)
            val filtered = categoryId?.let { id -> local.filter { it.categoryId == id } } ?: local
            if (filtered.isNotEmpty()) filtered else throw e
        }
    }

    override suspend fun getProduct(householdId: String, productId: String): Product {
        localDataSource.getProduct(householdId, productId)?.let { return it }
        return try {
            val remote = remoteDataSource.getProduct(householdId, productId).toDomain()
            val product = resolveRemoteImageCache(
                product = remote,
                local = localDataSource.getProduct(householdId, productId)
            )
            localDataSource.saveProduct(product)
            saveBarcodeCache(product)
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
        return try {
            val created = remoteDataSource.addProduct(householdId, request).toDomain()
            val product = uploadLocalImageImmediatelyOrQueue(
                householdId = householdId,
                productId = created.id,
                localImagePath = localImagePath,
                fallback = created.copy(localImagePath = localImagePath)
            )
            localDataSource.saveProduct(product)
            saveBarcodeCache(product)
            product
        } catch (e: Exception) {
            val localProduct = request.toLocalProduct(
                householdId = householdId,
                id = generateUuid(),
                localImagePath = localImagePath
            )
            localDataSource.saveProduct(localProduct)
            saveBarcodeCache(localProduct)
            queueAction(
                type = SyncActionType.ADD_PRODUCT,
                entityId = localProduct.id,
                householdId = householdId,
                payload = json.encodeToString(PendingCreateProductPayloadDto(request))
            )
            queueLocalImageUploadIfNeeded(householdId, localProduct.id, localImagePath)
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
        if (hasPendingActionForProduct(householdId, productId)) {
            return updateLocalAndQueue(householdId, productId, request, localImagePath)
        }
        return try {
            val updated = remoteDataSource.updateProduct(householdId, productId, request).toDomain()
            val product = updated.copy(
                localImagePath = when {
                    request.clearImage -> null
                    localImagePath != null -> localImagePath
                    else -> localDataSource.getProduct(householdId, productId)?.localImagePath
                }
            )
                .let { fallback ->
                    uploadLocalImageImmediatelyOrQueue(
                        householdId = householdId,
                        productId = productId,
                        localImagePath = localImagePath.takeUnless { request.clearImage },
                        fallback = fallback
                    )
                }
            localDataSource.saveProduct(product)
            saveBarcodeCache(product)
            product
        } catch (e: Exception) {
            updateLocalAndQueue(householdId, productId, request, localImagePath)
        }
    }

    override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product {
        val request = ConsumeProductRequestDto(amount = amount)
        if (hasPendingActionForProduct(householdId, productId)) {
            return consumeLocalAndQueue(householdId, productId, request)
        }
        return try {
            val product = remoteDataSource.consumeProduct(
                householdId = householdId,
                productId = productId,
                request = request
            ).toDomain()
            localDataSource.saveProduct(product)
            saveBarcodeCache(product)
            product
        } catch (e: Exception) {
            consumeLocalAndQueue(householdId, productId, request)
        }
    }

    override suspend fun deleteProduct(householdId: String, productId: String) {
        if (hasPendingAdd(householdId, productId)) {
            syncQueue.getPendingActions()
                .filter { it.householdId == householdId && it.entityId == productId }
                .forEach { syncQueue.removePendingAction(it.id) }
            localDataSource.deleteProduct(productId)
            return
        }
        removePendingActionsForProduct(householdId, productId)
        localDataSource.deleteProduct(productId)
        try {
            remoteDataSource.deleteProduct(householdId, productId)
        } catch (e: Exception) {
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

    override suspend fun upsertCachedProduct(product: Product): Product {
        val local = localDataSource.getProduct(product.householdId, product.id)
        if (local != null && hasPendingActionForProduct(product.householdId, product.id)) {
            saveBarcodeCache(local)
            return local
        }
        val cached = resolveRemoteImageCache(
            product = product,
            local = local
        )
        localDataSource.saveProduct(cached)
        saveBarcodeCache(cached)
        return cached
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
        saveBarcodeCache(updated)
        queueAction(
            type = SyncActionType.UPDATE_PRODUCT,
            entityId = productId,
            householdId = householdId,
            payload = json.encodeToString(PendingUpdateProductPayloadDto(request))
        )
        queueLocalImageUploadIfNeeded(householdId, productId, localImagePath)
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
        saveBarcodeCache(updated)
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
                .filter { it.householdId == householdId && it.type.isProductAction }
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
                val pendingLocal = localDataSource.getProduct(action.householdId, action.entityId)
                localDataSource.saveProduct(
                    (pendingLocal
                        ?.copy(
                            id = product.id,
                            imageUrl = product.imageUrl,
                            localImagePath = pendingLocal.localImagePath,
                            addedByUserId = product.addedByUserId,
                            createdAt = product.createdAt
                        )
                        ?: product)
                        .also { saveBarcodeCache(it) }
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
                payload.localImagePath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { queueLocalImageUploadIfAbsent(action.householdId, product.id, it) }
            }
            SyncActionType.UPDATE_PRODUCT -> {
                val payload = decodeUpdatePayload(action.payload)
                val updated = runCatching {
                    remoteDataSource.updateProduct(
                        action.householdId,
                        resolvedProductId,
                        payload.request
                    ).toDomain()
                }.getOrElse { error ->
                    if (!error.isNotFound()) throw error
                    val local = localDataSource.getProduct(action.householdId, resolvedProductId) ?: return
                    recreateRemoteProductFromLocal(
                        householdId = action.householdId,
                        oldProductId = resolvedProductId,
                        completedActionId = action.id,
                        idMappings = idMappings,
                        local = local,
                        request = payload.request.toCreateRequest(local),
                        localImagePath = payload.localImagePath ?: local.localImagePath
                    )
                    return
                }
                val product = updated.copy(
                    localImagePath = localDataSource.getProduct(action.householdId, resolvedProductId)?.localImagePath
                )
                localDataSource.saveProduct(product)
                saveBarcodeCache(product)
                payload.localImagePath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { queueLocalImageUploadIfAbsent(action.householdId, resolvedProductId, it) }
            }
            SyncActionType.CONSUME_PRODUCT -> {
                val product = remoteDataSource.consumeProduct(
                    householdId = action.householdId,
                    productId = resolvedProductId,
                    request = json.decodeFromString<ConsumeProductRequestDto>(action.payload)
                ).toDomain()
                localDataSource.saveProduct(product)
                saveBarcodeCache(product)
            }
            SyncActionType.DELETE_PRODUCT -> {
                runCatching {
                    remoteDataSource.deleteProduct(action.householdId, resolvedProductId)
                }.getOrElse { error ->
                    if (!error.isNotFound()) throw error
                }
                localDataSource.deleteProduct(resolvedProductId)
            }
            SyncActionType.UPLOAD_PRODUCT_IMAGE -> {
                val payload = decodeUploadPayload(action.payload)
                val current = localDataSource.getProduct(action.householdId, resolvedProductId)
                    ?: return
                val product = runCatching {
                    uploadLocalImageIfNeeded(
                        householdId = action.householdId,
                        productId = resolvedProductId,
                        localImagePath = payload.localImagePath,
                        fallback = current
                    )
                }.getOrElse { error ->
                    if (!error.isNotFound()) throw error
                    recreateRemoteProductFromLocal(
                        householdId = action.householdId,
                        oldProductId = resolvedProductId,
                        completedActionId = action.id,
                        idMappings = idMappings,
                        local = current,
                        request = current.toCreateRequest(imageUrl = null),
                        localImagePath = payload.localImagePath
                    )
                    return
                }
                localDataSource.saveProduct(product)
                saveBarcodeCache(product)
            }
            SyncActionType.CREATE_CATEGORY,
            SyncActionType.UPDATE_CATEGORY,
            SyncActionType.ARCHIVE_CATEGORY -> Unit
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

    private suspend fun recreateRemoteProductFromLocal(
        householdId: String,
        oldProductId: String,
        completedActionId: String,
        idMappings: MutableMap<String, String>,
        local: Product,
        request: CreateProductRequestDto,
        localImagePath: String?
    ): Product {
        val created = remoteDataSource.addProduct(householdId, request).toDomain()
        val previewPath = localImagePath?.takeIf { it.isNotBlank() } ?: local.localImagePath
        val recreated = created.copy(localImagePath = previewPath)
        localDataSource.deleteProduct(oldProductId)
        localDataSource.saveProduct(recreated)
        saveBarcodeCache(recreated)

        if (created.id != oldProductId) {
            idMappings[oldProductId] = created.id
            remapQueuedProductId(
                householdId = householdId,
                oldProductId = oldProductId,
                newProductId = created.id,
                completedActionId = completedActionId
            )
        }
        previewPath?.takeIf { it.isNotBlank() }?.let {
            queueLocalImageUploadIfAbsent(householdId, created.id, it)
        }
        return recreated
    }

    private suspend fun mergeRemoteWithPendingLocal(householdId: String, remote: List<Product>): List<Product> {
        val pending = syncQueue.getPendingActions().filter { it.householdId == householdId && it.type.isProductAction }
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

    private suspend fun resolveRemoteImageCache(householdId: String, remote: List<Product>): List<Product> {
        val localById = localDataSource.getProducts(householdId).associateBy { it.id }
        return remote.map { product -> resolveRemoteImageCache(product, localById[product.id]) }
    }

    private suspend fun resolveRemoteImageCache(product: Product, local: Product?): Product {
        val imageUrl = product.imageUrl?.takeIf { it.isNotBlank() }
            ?: return product.copy(localImagePath = null)
        val cachedPath = imageLocalCache.localPathForRemoteImage(product.id, imageUrl)
            ?: return product.copy(localImagePath = null)

        if (imageLocalCache.exists(cachedPath)) {
            return product.copy(localImagePath = cachedPath)
        }

        val downloaded = runCatching { remoteDataSource.downloadProductImage(imageUrl) }.getOrNull()
        if (downloaded?.isNotEmpty() == true && imageLocalCache.write(cachedPath, downloaded)) {
            return product.copy(localImagePath = cachedPath)
        }

        return product.copy(localImagePath = null)
    }

    private suspend fun hasPendingActionForProduct(householdId: String, productId: String): Boolean =
        syncQueue.getPendingActions().any { it.householdId == householdId && it.entityId == productId }

    private suspend fun hasPendingAdd(householdId: String, productId: String): Boolean =
        syncQueue.getPendingActions()
            .any { it.householdId == householdId && it.entityId == productId && it.type == SyncActionType.ADD_PRODUCT }

    private suspend fun removePendingActionsForProduct(householdId: String, productId: String) {
        syncQueue.getPendingActions()
            .filter { it.householdId == householdId && it.entityId == productId }
            .forEach { syncQueue.removePendingAction(it.id) }
    }

    private suspend fun queueLocalImageUploadIfNeeded(
        householdId: String,
        productId: String,
        localImagePath: String?
    ) {
        if (localImagePath.isNullOrBlank()) return
        queueAction(
            type = SyncActionType.UPLOAD_PRODUCT_IMAGE,
            entityId = productId,
            householdId = householdId,
            payload = json.encodeToString(
                PendingUploadProductImagePayloadDto(
                    localImagePath = localImagePath
                )
            )
        )
    }

    private suspend fun queueLocalImageUploadIfAbsent(
        householdId: String,
        productId: String,
        localImagePath: String
    ) {
        if (syncQueue.getPendingActions().any {
            it.householdId == householdId &&
                it.entityId == productId &&
                it.type == SyncActionType.UPLOAD_PRODUCT_IMAGE
        }) {
            return
        }
        queueLocalImageUploadIfNeeded(householdId, productId, localImagePath)
    }

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

    private fun Product.toCreateRequest(imageUrl: String? = this.imageUrl): CreateProductRequestDto =
        CreateProductRequestDto(
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
        val image = imageFileReader.read(path)
            ?: throw IllegalStateException("Product image file is not readable: $path")
        return remoteDataSource.uploadProductImage(householdId, productId, image).toDomain()
            .copy(localImagePath = path)
    }

    private suspend fun uploadLocalImageImmediatelyOrQueue(
        householdId: String,
        productId: String,
        localImagePath: String?,
        fallback: Product
    ): Product {
        if (localImagePath.isNullOrBlank()) return fallback
        return runCatching {
            uploadLocalImageIfNeeded(householdId, productId, localImagePath, fallback)
        }.getOrElse {
            queueLocalImageUploadIfAbsent(householdId, productId, localImagePath)
            fallback
        }
    }

    private suspend fun saveBarcodeCache(product: Product) {
        val barcode = product.barcode?.trim()?.takeIf { it.isNotBlank() } ?: return
        barcodeLocalDataSource?.saveBarcode(
            CachedBarcodeProduct(
                householdId = product.householdId,
                barcode = barcode,
                name = product.name,
                brand = product.brand,
                category = product.category.name,
                categoryId = product.categoryId,
                categoryName = product.categoryName,
                packageQuantity = product.packageAmount,
                packageQuantityUnit = product.packageUnit?.name,
                ingredients = product.ingredientsText,
                imageUrl = product.imageUrl,
                localImagePath = product.localImagePath,
                caloriesKcal = product.calories,
                proteinGrams = product.protein,
                fatGrams = product.fat,
                carbohydratesGrams = product.carbs,
                source = "LOCAL_DATABASE",
                updatedAt = nowMillis()
            )
        )
    }

    private fun decodeCreatePayload(payload: String): PendingCreateProductPayloadDto =
        runCatching { json.decodeFromString<PendingCreateProductPayloadDto>(payload) }
            .getOrElse { PendingCreateProductPayloadDto(json.decodeFromString(payload), null) }

    private fun decodeUpdatePayload(payload: String): PendingUpdateProductPayloadDto =
        runCatching { json.decodeFromString<PendingUpdateProductPayloadDto>(payload) }
            .getOrElse { PendingUpdateProductPayloadDto(json.decodeFromString(payload), null) }

    private fun decodeUploadPayload(payload: String): PendingUploadProductImagePayloadDto =
        runCatching { json.decodeFromString<PendingUploadProductImagePayloadDto>(payload) }
            .getOrElse {
                PendingUploadProductImagePayloadDto(
                    localImagePath = decodeUpdatePayload(payload).localImagePath.orEmpty()
                )
            }

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

    private fun Throwable.isNotFound(): Boolean =
        this is ApiException && statusCode == 404

    private val SyncActionType.isProductAction: Boolean
        get() = this == SyncActionType.ADD_PRODUCT ||
            this == SyncActionType.UPDATE_PRODUCT ||
            this == SyncActionType.CONSUME_PRODUCT ||
            this == SyncActionType.DELETE_PRODUCT ||
            this == SyncActionType.UPLOAD_PRODUCT_IMAGE
}
