package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.core.local.InMemoryPersistentKeyValueStore
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class LikedRecipeLocalDataSourceTest {

    @Test
    fun `persists liked recipes by household in shared store`() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val dataSource = LikedRecipeLocalDataSource(store)
        val firstHouseholdRecipe = recipe("Рис с овощами")
        val secondHouseholdRecipe = recipe("Паста")

        dataSource.setLikedRecipe("household-1", firstHouseholdRecipe, liked = true)
        dataSource.setLikedRecipe("household-2", secondHouseholdRecipe, liked = true)

        val reloadedDataSource = LikedRecipeLocalDataSource(store)
        assertEquals(listOf(firstHouseholdRecipe), reloadedDataSource.getLikedRecipes("household-1"))
        assertEquals(listOf(secondHouseholdRecipe), reloadedDataSource.getLikedRecipes("household-2"))
    }

    @Test
    fun `unlikes recipe by stable local identity`() = runTest {
        val dataSource = LikedRecipeLocalDataSource(InMemoryPersistentKeyValueStore())
        val recipe = recipe("Рис с овощами")

        dataSource.setLikedRecipe("household-1", recipe, liked = true)
        dataSource.setLikedRecipe("household-1", recipe.copy(calories = 400), liked = false)

        assertEquals(emptyList(), dataSource.getLikedRecipes("household-1"))
    }

    private fun recipe(title: String): Recipe =
        Recipe(
            title = title,
            ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
            steps = listOf("Сварить рис"),
            time = "20 минут",
            calories = 300
        )
}
