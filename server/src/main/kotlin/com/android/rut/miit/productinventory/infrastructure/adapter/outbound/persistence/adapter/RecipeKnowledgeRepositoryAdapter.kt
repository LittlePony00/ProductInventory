package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.RecipeDocumentEntity
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaRecipeDocumentRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class RecipeKnowledgeRepositoryAdapter(
    private val jpaRepository: JpaRecipeDocumentRepository,
    private val objectMapper: ObjectMapper
) : IRecipeKnowledgeRepository {

    override fun findAll(): List<RecipeDocument> =
        jpaRepository.findAll().map { it.toDomain(objectMapper) }
}

private fun RecipeDocumentEntity.toDomain(objectMapper: ObjectMapper): RecipeDocument =
    RecipeDocument(
        id = id,
        title = title,
        ingredients = objectMapper.readValue(ingredientsJson, recipeIngredientsType),
        steps = objectMapper.readValue(stepsJson, stringListType),
        time = time,
        calories = calories,
        requiredIngredients = requiredIngredients.splitCsv().map { it.lowercase() }.toSet(),
        categories = categories.splitCsv().mapNotNull { runCatching { ProductCategory.valueOf(it) }.getOrNull() }.toSet(),
        rules = objectMapper.readValue(rulesJson, stringListType)
    )

private fun String.splitCsv(): List<String> =
    split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private val recipeIngredientsType = object : TypeReference<List<RecipeIngredient>>() {}
private val stringListType = object : TypeReference<List<String>>() {}
