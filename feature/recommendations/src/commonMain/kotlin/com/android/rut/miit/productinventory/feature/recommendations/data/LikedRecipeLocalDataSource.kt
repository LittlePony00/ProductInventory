package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.core.local.PersistentKeyValueStore
import com.android.rut.miit.productinventory.core.local.persistentLocalJson
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LikedRecipeLocalDataSource(
    private val store: PersistentKeyValueStore,
    private val json: Json = persistentLocalJson
) {
    private val mutex = Mutex()

    suspend fun getLikedRecipes(householdId: String): List<Recipe> =
        mutex.withLock {
            readCache().recipesByHousehold[householdId].orEmpty()
        }

    suspend fun setLikedRecipe(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> =
        mutex.withLock {
            val cache = readCache()
            val recipes = cache.recipesByHousehold[householdId].orEmpty()
            val nextRecipes = if (liked) {
                recipes.filterNot { it.localIdentity() == recipe.localIdentity() } + recipe
            } else {
                recipes.filterNot { it.localIdentity() == recipe.localIdentity() }
            }
            writeCache(cache.copy(recipesByHousehold = cache.recipesByHousehold + (householdId to nextRecipes)))
            nextRecipes
        }

    private suspend fun readCache(): LikedRecipeCache {
        val raw = store.read(LIKED_RECIPES_KEY) ?: return LikedRecipeCache()
        return runCatching { json.decodeFromString<LikedRecipeCache>(raw) }
            .getOrElse {
                store.remove(LIKED_RECIPES_KEY)
                LikedRecipeCache()
            }
    }

    private suspend fun writeCache(cache: LikedRecipeCache) {
        store.write(LIKED_RECIPES_KEY, json.encodeToString(cache))
    }

    private companion object {
        const val LIKED_RECIPES_KEY = "liked_recipes_v1"
    }
}

@Serializable
private data class LikedRecipeCache(
    val recipesByHousehold: Map<String, List<Recipe>> = emptyMap()
)
