package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentCategoryLocalDataSource(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) : CategoryLocalDataSource {

    private val mutex = Mutex()

    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> {
        return mutex.withLock {
            readCache().categoriesByHousehold[householdId]
                .orEmpty()
                .asSequence()
                .filter { includeArchived || !it.archived }
                .map { it.toDomain() }
                .toList()
        }
    }

    override suspend fun saveCategories(householdId: String, categories: List<ProductCategoryOption>) {
        mutex.withLock {
            val cache = readCache()
            writeCache(
                cache.copy(
                    categoriesByHousehold = cache.categoriesByHousehold + (householdId to categories.map { it.toRecord(householdId) })
                )
            )
        }
    }

    override suspend fun saveCategory(category: ProductCategoryOption) {
        val householdId = category.householdId ?: return
        mutex.withLock {
            val cache = readCache()
            val current = cache.categoriesByHousehold[householdId].orEmpty()
            writeCache(
                cache.copy(
                    categoriesByHousehold = cache.categoriesByHousehold + (
                        householdId to (current.filterNot { it.id == category.id } + category.toRecord(householdId))
                        )
                )
            )
        }
    }

    override suspend fun archiveCategory(householdId: String, categoryId: String) {
        mutex.withLock {
            val cache = readCache()
            val current = cache.categoriesByHousehold[householdId].orEmpty()
            writeCache(
                cache.copy(
                    categoriesByHousehold = cache.categoriesByHousehold + (
                        householdId to current.map { if (it.id == categoryId) it.copy(archived = true) else it }
                        )
                )
            )
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        mutex.withLock {
            val cache = readCache()
            writeCache(
                cache.copy(
                    categoriesByHousehold = cache.categoriesByHousehold.mapValues { (_, categories) ->
                        categories.filterNot { it.id == categoryId }
                    }
                )
            )
        }
    }

    private suspend fun readCache(): CategoryCache {
        val raw = store.read(CATEGORIES_KEY) ?: return CategoryCache()
        val decoded = runCatching { json.decodeFromString<CategoryCache>(raw) }
        return decoded.getOrElse {
            store.remove(CATEGORIES_KEY)
            CategoryCache()
        }
    }

    private suspend fun writeCache(cache: CategoryCache) {
        store.write(CATEGORIES_KEY, json.encodeToString(cache))
    }

    private fun CategoryRecord.toDomain() = ProductCategoryOption(
        id = id,
        householdId = householdId,
        code = code?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
        name = name,
        system = system,
        archived = archived,
        createdAt = createdAt
    )

    private fun ProductCategoryOption.toRecord(fallbackHouseholdId: String) = CategoryRecord(
        id = id,
        householdId = householdId ?: fallbackHouseholdId,
        code = code?.name,
        name = name,
        system = system,
        archived = archived,
        createdAt = createdAt
    )

    private companion object {
        const val CATEGORIES_KEY = "local_categories_v1"
    }
}

@Serializable
private data class CategoryCache(
    val categoriesByHousehold: Map<String, List<CategoryRecord>> = emptyMap()
)

@Serializable
private data class CategoryRecord(
    val id: String,
    val householdId: String?,
    val code: String?,
    val name: String,
    val system: Boolean,
    val archived: Boolean,
    val createdAt: String
)
