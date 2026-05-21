package com.android.rut.miit.productinventory.application.mapper

import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeDtoMapperTest {

    @Test
    fun `maps recipe to strict response contract`() {
        val response = Recipe(
            title = "Rice Bowl",
            ingredients = listOf(RecipeIngredient(name = "rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300
        ).toResponse()

        assertEquals("Rice Bowl", response.title)
        assertEquals("rice", response.ingredients.single().name)
        assertEquals("1 cup", response.ingredients.single().amount)
        assertEquals(listOf("Cook rice"), response.steps)
        assertEquals("15 minutes", response.time)
        assertEquals(300, response.calories)
        assertEquals(true, response.caloriesKnown)
        val fields = ObjectMapper().registerKotlinModule().valueToTree<com.fasterxml.jackson.databind.JsonNode>(response)
            .fieldNames()
            .asSequence()
            .toSet()
        assertEquals(true, fields.containsAll(setOf("title", "ingredients", "steps", "time", "calories", "caloriesKnown")))
        assertEquals(true, fields.containsAll(setOf("score", "reasons", "warnings", "aiAssisted", "aiGenerated")))
    }

    @Test
    fun `maps ai-assisted recipe recommendation fields`() {
        val response = RecipeRecommendation(
            title = "Rice Bowl",
            ingredients = listOf(RecipeIngredient(name = "rice", amount = "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300,
            source = RecipeSource.AI_ASSISTED,
            aiAssisted = true,
            aiGenerated = false
        ).toResponse()

        assertEquals(RecipeSource.AI_ASSISTED, response.source)
        assertEquals(true, response.aiAssisted)
        assertEquals(false, response.aiGenerated)
    }
}
