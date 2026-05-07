package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Recipe
import java.util.UUID

interface IRecommendationService {
    fun getRecipes(userId: UUID, householdId: UUID): List<Recipe>
}
