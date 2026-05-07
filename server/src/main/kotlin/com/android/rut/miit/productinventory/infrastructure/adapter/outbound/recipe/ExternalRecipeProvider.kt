package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ExternalRecipeProvider : IRecipeProvider {

    private val log = LoggerFactory.getLogger(ExternalRecipeProvider::class.java)

    override fun findByIngredients(ingredients: List<String>): List<Recipe> {
        // TODO: integrate with external recipe API
        log.info("Recipe search for ingredients: ${ingredients.take(10)}")

        return listOf(
            Recipe(
                id = "demo-1",
                title = "Quick Salad",
                description = "A simple salad from available ingredients",
                ingredients = ingredients.take(5),
                instructions = "1. Wash and chop ingredients\n2. Mix in a bowl\n3. Add dressing",
                imageUrl = null
            )
        )
    }
}
