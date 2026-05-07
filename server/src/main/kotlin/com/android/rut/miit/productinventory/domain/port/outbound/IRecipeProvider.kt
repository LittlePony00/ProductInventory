package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Recipe

interface IRecipeProvider {
    fun findByIngredients(ingredients: List<String>): List<Recipe>
}
