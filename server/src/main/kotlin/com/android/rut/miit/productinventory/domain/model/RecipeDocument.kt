package com.android.rut.miit.productinventory.domain.model

data class RecipeDocument(
    val id: String,
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val calories: Int,
    val requiredIngredients: Set<String>,
    val categories: Set<ProductCategory>,
    val rules: List<String>
) {
    fun toRecipe(): Recipe = Recipe(
        title = title,
        ingredients = ingredients,
        steps = steps,
        time = time,
        calories = calories
    )
}

data class RecipeDocumentMatch(
    val document: RecipeDocument,
    val score: Double,
    val matchedProducts: List<Product>,
    val appliedRules: List<String>
)
