package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.RecipeDocumentEntity
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaRecipeDocumentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class RecipeKnowledgeRepositoryAdapterTest {

    @Autowired
    private lateinit var jpaRepository: JpaRecipeDocumentRepository

    @Test
    fun `maps persisted recipe document to domain`() {
        jpaRepository.save(
            RecipeDocumentEntity(
                id = "rice-bowl",
                title = "Rice Bowl",
                ingredientsJson = """[{"name":"rice","amount":"1 cup"}]""",
                stepsJson = """["Cook rice"]""",
                time = "15 minutes",
                calories = 300,
                requiredIngredients = "rice, vegetables",
                categories = "CEREALS,VEGETABLES_FRUITS",
                rulesJson = """["Use expiring vegetables first"]"""
            )
        )
        val adapter = RecipeKnowledgeRepositoryAdapter(jpaRepository, ObjectMapper().registerKotlinModule())

        val document = adapter.findAll().single()

        assertEquals("rice-bowl", document.id)
        assertEquals("Rice Bowl", document.title)
        assertEquals("rice", document.ingredients.single().name)
        assertEquals("1 cup", document.ingredients.single().amount)
        assertEquals(listOf("Cook rice"), document.steps)
        assertEquals(setOf("rice", "vegetables"), document.requiredIngredients)
        assertEquals(setOf(ProductCategory.CEREALS, ProductCategory.VEGETABLES_FRUITS), document.categories)
        assertEquals(listOf("Use expiring vegetables first"), document.rules)
    }
}
