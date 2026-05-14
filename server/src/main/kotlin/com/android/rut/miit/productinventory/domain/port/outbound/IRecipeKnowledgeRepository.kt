package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.RecipeDocument

interface IRecipeKnowledgeRepository {
    fun findAll(): List<RecipeDocument>
}
