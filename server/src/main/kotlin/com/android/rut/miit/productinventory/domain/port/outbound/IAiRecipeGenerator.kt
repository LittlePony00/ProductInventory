package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.Recipe

interface IAiRecipeGenerator {
    fun generateRecipe(context: AiRecipeGenerationContext): Recipe?
}
