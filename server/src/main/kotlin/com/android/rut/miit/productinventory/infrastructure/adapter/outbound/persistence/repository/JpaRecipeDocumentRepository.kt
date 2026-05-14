package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.RecipeDocumentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface JpaRecipeDocumentRepository : JpaRepository<RecipeDocumentEntity, String>
