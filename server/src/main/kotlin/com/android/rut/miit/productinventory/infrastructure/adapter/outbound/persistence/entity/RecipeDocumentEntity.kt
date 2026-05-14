package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "recipe_documents")
class RecipeDocumentEntity(
    @Id
    val id: String = "",

    @Column(nullable = false)
    var title: String = "",

    @Column(name = "ingredients_json", nullable = false, columnDefinition = "TEXT")
    var ingredientsJson: String = "[]",

    @Column(name = "steps_json", nullable = false, columnDefinition = "TEXT")
    var stepsJson: String = "[]",

    @Column(name = "time_text", nullable = false)
    var time: String = "",

    @Column(nullable = false)
    var calories: Int = 0,

    @Column(name = "required_ingredients", nullable = false, columnDefinition = "TEXT")
    var requiredIngredients: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var categories: String = "",

    @Column(name = "rules_json", nullable = false, columnDefinition = "TEXT")
    var rulesJson: String = "[]"
)
