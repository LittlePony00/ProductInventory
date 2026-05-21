package com.android.rut.miit.productinventory.feature.recommendations.data.mappers

import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeIngredientDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeMapperTest {

    @Test
    fun `maps strict recipe response dto to domain`() {
        val recipe = RecipeResponseDto(
            title = "Rice Bowl",
            ingredients = listOf(RecipeIngredientDto(name = "rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300
        ).toDomain()

        assertEquals("Rice Bowl", recipe.title)
        assertEquals("rice", recipe.ingredients.single().name)
        assertEquals("1 cup", recipe.ingredients.single().amount)
        assertEquals(listOf("Cook rice"), recipe.steps)
        assertEquals("15 minutes", recipe.time)
        assertEquals(300, recipe.calories)
        assertEquals(true, recipe.caloriesKnown)
        assertEquals(false, recipe.aiAssisted)
    }

    @Test
    fun `maps ai-assisted marker to domain`() {
        val recipe = RecipeResponseDto(
            title = "Rice Bowl",
            ingredients = listOf(RecipeIngredientDto(name = "rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300,
            source = "AI_ASSISTED",
            aiAssisted = true,
            aiGenerated = false
        ).toDomain()

        assertEquals("AI_ASSISTED", recipe.source)
        assertEquals(true, recipe.aiAssisted)
        assertEquals(false, recipe.aiGenerated)
    }
}
