package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.local.CategoryLocalDataSource
import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.CreateCategoryRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingCreateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingArchiveCategoryPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingCreateCategoryPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUpdateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUpdateCategoryPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateCategoryRequestDto
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepositoryImpl(
    private val remoteDataSource: CategoryRemoteDataSource,
    private val localDataSource: CategoryLocalDataSource,
    private val productLocalDataSource: ProductLocalDataSource,
    private val syncQueue: SyncQueue,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : CategoryRepository {

    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
        (ProductCategoryOption.systemDefaults() + localDataSource.getCategories(householdId, includeArchived))
            .dedupeAndSort(includeArchived)

    override suspend fun refreshCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> {
        runCatching { syncPendingCategoryActions(householdId) }
        return try {
            val remoteCategories = remoteDataSource.getCategories(householdId, includeArchived).map { it.toDomain() }
            val pendingLocal = pendingLocalCategories(householdId, includeArchived)
            val mergedCustom = (remoteCategories.filterNot { it.system } + pendingLocal)
                .distinctBy { it.id }
            localDataSource.saveCategories(householdId, mergedCustom)
            (ProductCategoryOption.systemDefaults() + mergedCustom).dedupeAndSort(includeArchived)
        } catch (e: Exception) {
            getCategories(householdId, includeArchived)
        }
    }

    override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption {
        val normalizedName = name.trim()
        return try {
            val category = remoteDataSource
                .createCategory(householdId, CreateCategoryRequestDto(normalizedName))
                .toDomain()
            localDataSource.saveCategory(category)
            category
        } catch (e: Exception) {
            val category = ProductCategoryOption(
                id = "local-${generateUuid()}",
                householdId = householdId,
                name = normalizedName,
                system = false,
                createdAt = nowMillis().toString()
            )
            localDataSource.saveCategory(category)
            queueAction(
                type = SyncActionType.CREATE_CATEGORY,
                entityId = category.id,
                householdId = householdId,
                payload = json.encodeToString(
                    PendingCreateCategoryPayloadDto(
                        name = normalizedName,
                        localCategoryId = category.id
                    )
                )
            )
            category
        }
    }

    override suspend fun updateCategory(
        householdId: String,
        categoryId: String,
        name: String
    ): ProductCategoryOption {
        val normalizedName = name.trim()
        val current = getCategories(householdId, includeArchived = true).first { it.id == categoryId }
        val updated = current.copy(name = normalizedName)
        localDataSource.saveCategory(updated)
        return try {
            val remote = remoteDataSource.updateCategory(
                householdId,
                categoryId,
                UpdateCategoryRequestDto(normalizedName)
            ).toDomain()
            localDataSource.saveCategory(remote)
            remote
        } catch (e: Exception) {
            queueAction(
                type = SyncActionType.UPDATE_CATEGORY,
                entityId = categoryId,
                householdId = householdId,
                payload = json.encodeToString(PendingUpdateCategoryPayloadDto(normalizedName))
            )
            updated
        }
    }

    override suspend fun archiveCategory(householdId: String, categoryId: String) {
        localDataSource.archiveCategory(householdId, categoryId)
        try {
            remoteDataSource.archiveCategory(householdId, categoryId)
        } catch (e: Exception) {
            queueAction(
                type = SyncActionType.ARCHIVE_CATEGORY,
                entityId = categoryId,
                householdId = householdId,
                payload = json.encodeToString(PendingArchiveCategoryPayloadDto)
            )
        }
    }

    private suspend fun syncPendingCategoryActions(householdId: String) {
        val idMappings = mutableMapOf<String, String>()
        syncQueue.getPendingActions()
            .filter { it.householdId == householdId && it.type.isCategoryAction }
            .sortedBy { it.createdAt }
            .forEach { action ->
                executePendingCategoryAction(action, idMappings)
                syncQueue.removePendingAction(action.id)
            }
    }

    private suspend fun executePendingCategoryAction(
        action: PendingSyncAction,
        idMappings: MutableMap<String, String>
    ) {
        val resolvedCategoryId = idMappings[action.entityId] ?: action.entityId
        when (action.type) {
            SyncActionType.CREATE_CATEGORY -> {
                val payload = json.decodeFromString<PendingCreateCategoryPayloadDto>(action.payload)
                val remote = remoteDataSource.createCategory(
                    action.householdId,
                    CreateCategoryRequestDto(payload.name)
                ).toDomain()
                remapCreatedCategory(action.householdId, action.entityId, remote)
                localDataSource.deleteCategory(action.entityId)
                localDataSource.saveCategory(remote)
                idMappings[action.entityId] = remote.id
            }
            SyncActionType.UPDATE_CATEGORY -> {
                val payload = json.decodeFromString<PendingUpdateCategoryPayloadDto>(action.payload)
                val remote = remoteDataSource.updateCategory(
                    action.householdId,
                    resolvedCategoryId,
                    UpdateCategoryRequestDto(payload.name)
                ).toDomain()
                localDataSource.saveCategory(remote)
            }
            SyncActionType.ARCHIVE_CATEGORY -> {
                remoteDataSource.archiveCategory(action.householdId, resolvedCategoryId)
                localDataSource.archiveCategory(action.householdId, resolvedCategoryId)
            }
            else -> Unit
        }
    }

    private suspend fun pendingLocalCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> {
        val pendingIds = syncQueue.getPendingActions()
            .filter { it.householdId == householdId && it.type.isCategoryAction }
            .mapTo(mutableSetOf()) { it.entityId }
        if (pendingIds.isEmpty()) return emptyList()
        return localDataSource.getCategories(householdId, includeArchived)
            .filter { it.id in pendingIds }
    }

    private suspend fun remapCreatedCategory(
        householdId: String,
        oldId: String,
        remoteCategory: ProductCategoryOption
    ) {
        productLocalDataSource.remapCategoryId(
            householdId = householdId,
            oldCategoryId = oldId,
            newCategoryId = remoteCategory.id,
            newCategoryName = remoteCategory.name
        )
        syncQueue.getPendingActions()
            .filter { it.householdId == householdId }
            .forEach { action ->
                remapPendingActionCategory(action, oldId, remoteCategory)?.let { syncQueue.updatePendingAction(it) }
            }
    }

    private fun remapPendingActionCategory(
        action: PendingSyncAction,
        oldId: String,
        remoteCategory: ProductCategoryOption
    ): PendingSyncAction? =
        when (action.type) {
            SyncActionType.UPDATE_CATEGORY,
            SyncActionType.ARCHIVE_CATEGORY -> {
                if (action.entityId == oldId) action.copy(entityId = remoteCategory.id) else null
            }
            SyncActionType.ADD_PRODUCT -> remapCreateProductPayload(action, oldId, remoteCategory.id)
            SyncActionType.UPDATE_PRODUCT -> remapUpdateProductPayload(action, oldId, remoteCategory.id)
            SyncActionType.CONSUME_PRODUCT,
            SyncActionType.DELETE_PRODUCT,
            SyncActionType.UPLOAD_PRODUCT_IMAGE,
            SyncActionType.CREATE_CATEGORY -> null
        }

    private fun remapCreateProductPayload(action: PendingSyncAction, oldId: String, newId: String): PendingSyncAction? {
        val payload = decodeCreateProductPayload(action.payload)
        if (payload.request.categoryId != oldId) return null
        return action.copy(
            payload = json.encodeToString(
                payload.copy(request = payload.request.copy(categoryId = newId))
            )
        )
    }

    private fun remapUpdateProductPayload(action: PendingSyncAction, oldId: String, newId: String): PendingSyncAction? {
        val payload = decodeUpdateProductPayload(action.payload)
        if (payload.request.categoryId != oldId) return null
        return action.copy(
            payload = json.encodeToString(
                payload.copy(request = payload.request.copy(categoryId = newId))
            )
        )
    }

    private fun decodeCreateProductPayload(payload: String): PendingCreateProductPayloadDto =
        runCatching { json.decodeFromString<PendingCreateProductPayloadDto>(payload) }
            .getOrElse { PendingCreateProductPayloadDto(json.decodeFromString(payload), null) }

    private fun decodeUpdateProductPayload(payload: String): PendingUpdateProductPayloadDto =
        runCatching { json.decodeFromString<PendingUpdateProductPayloadDto>(payload) }
            .getOrElse { PendingUpdateProductPayloadDto(json.decodeFromString(payload), null) }

    private suspend fun queueAction(
        type: SyncActionType,
        entityId: String,
        householdId: String,
        payload: String
    ) {
        val createdAt = nowMillis()
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

    private fun List<ProductCategoryOption>.dedupeAndSort(includeArchived: Boolean): List<ProductCategoryOption> =
        distinctBy { it.id }
            .filter { includeArchived || !it.archived }
            .sortedWith(compareBy<ProductCategoryOption> { !it.system }.thenBy { it.name.lowercase() })

    private val SyncActionType.isCategoryAction: Boolean
        get() = this == SyncActionType.CREATE_CATEGORY ||
            this == SyncActionType.UPDATE_CATEGORY ||
            this == SyncActionType.ARCHIVE_CATEGORY

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
}
