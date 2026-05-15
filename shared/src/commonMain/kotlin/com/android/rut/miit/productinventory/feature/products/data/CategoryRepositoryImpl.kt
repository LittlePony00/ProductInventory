package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.CreateCategoryRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateCategoryRequestDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CategoryRepositoryImpl(
    private val remoteDataSource: CategoryRemoteDataSource
) : CategoryRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<CacheKey, List<ProductCategoryOption>>()

    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> {
        return try {
            remoteDataSource.getCategories(householdId, includeArchived)
                .map { it.toDomain() }
                .also { categories -> putCache(householdId, includeArchived, categories) }
        } catch (e: Exception) {
            getCache(householdId, includeArchived)
                ?: if (includeArchived) throw e else ProductCategoryOption.systemDefaults()
        }
    }

    override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption {
        val category = remoteDataSource.createCategory(householdId, CreateCategoryRequestDto(name)).toDomain()
        updateHouseholdCache(householdId) { categories ->
            (categories.filterNot { it.id == category.id } + category).sortForDisplay()
        }
        return category
    }

    override suspend fun updateCategory(
        householdId: String,
        categoryId: String,
        name: String
    ): ProductCategoryOption {
        val category = remoteDataSource.updateCategory(householdId, categoryId, UpdateCategoryRequestDto(name)).toDomain()
        updateHouseholdCache(householdId) { categories ->
            categories.map { if (it.id == category.id) category else it }.sortForDisplay()
        }
        return category
    }

    override suspend fun archiveCategory(householdId: String, categoryId: String) {
        remoteDataSource.archiveCategory(householdId, categoryId)
        updateHouseholdCache(householdId) { categories ->
            categories.mapNotNull {
                when {
                    it.id != categoryId -> it
                    it.archived -> it
                    else -> it.copy(archived = true)
                }
            }
        }
    }

    private suspend fun putCache(
        householdId: String,
        includeArchived: Boolean,
        categories: List<ProductCategoryOption>
    ) {
        mutex.withLock {
            cache[CacheKey(householdId, includeArchived)] = categories.sortForDisplay()
        }
    }

    private suspend fun getCache(householdId: String, includeArchived: Boolean): List<ProductCategoryOption>? =
        mutex.withLock {
            cache[CacheKey(householdId, includeArchived)]
        }

    private suspend fun updateHouseholdCache(
        householdId: String,
        transform: (List<ProductCategoryOption>) -> List<ProductCategoryOption>
    ) {
        mutex.withLock {
            listOf(false, true).forEach { includeArchived ->
                val key = CacheKey(householdId, includeArchived)
                val current = cache[key].orEmpty()
                val next = transform(current)
                    .filter { includeArchived || !it.archived }
                    .sortForDisplay()
                if (next.isNotEmpty()) cache[key] = next
            }
        }
    }

    private fun List<ProductCategoryOption>.sortForDisplay(): List<ProductCategoryOption> =
        sortedWith(compareBy<ProductCategoryOption> { !it.system }.thenBy { it.name.lowercase() })

    private data class CacheKey(
        val householdId: String,
        val includeArchived: Boolean
    )
}
