package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Recipe

interface IAiRecipeLocalizer {
    fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe?
    fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> =
        recipes.map(::localizeAndEnrichFoundRecipe)
}
