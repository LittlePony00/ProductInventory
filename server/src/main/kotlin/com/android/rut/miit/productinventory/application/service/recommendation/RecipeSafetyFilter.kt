package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.model.preferenceCategoryId
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class RecipeSafetyFilter {

    fun filterCandidates(
        candidates: List<RecipeCandidate>,
        preferences: UserFoodPreferences
    ): List<RecipeCandidate> =
        candidates.filter { candidate ->
            evaluate(
                recipe = candidate.document.toRecipe(),
                requiredIngredients = candidate.document.requiredIngredients,
                products = candidate.match.matchedProducts,
                recipeCategories = candidate.document.categories,
                preferences = preferences
            ).safe
        }

    fun evaluate(
        recipe: Recipe,
        requiredIngredients: Set<String>,
        products: List<Product>,
        recipeCategories: Set<ProductCategory> = emptySet(),
        preferences: UserFoodPreferences
    ): SafetyResult {
        val terms = (
            recipe.ingredients.map { it.name } +
                requiredIngredients +
                products.map { it.name } +
                products.mapNotNull { it.ingredientsText }
            )
            .flatMap(::ingredientTerms)
            .toSet()

        val allergyHits = preferences.allergies.filter { preference ->
            terms.matchesAnyPreferenceTerm(ingredientTerms(preference))
        }
        val dislikedHits = preferences.dislikedIngredients.filter { preference ->
            terms.matchesAnyPreferenceTerm(ingredientTerms(preference))
        }
        val avoidedFreeformProductHits = preferences.avoidedProducts.filter { preference ->
            terms.matchesAnyPreferenceTerm(ingredientTerms(preference))
        }
        val dietHits = preferences.dietaryRestrictions
            .mapNotNull { restriction -> restriction.violationTerms().firstOrNull { terms.matchesAnyPreferenceTerm(setOf(it)) }?.let { restriction } }
            .toSet()
        val avoidedProductHits = products
            .filter { it.id in preferences.avoidedProductIds }
            .map(Product::name)
            .toSet() + avoidedFreeformProductHits
        val categoryNamesById = products
            .associate { it.preferenceCategoryId() to (it.categoryName ?: it.category.name) } +
            recipeCategories.associate {
                val categoryId = SystemCategoryCatalog.idFor(it)
                categoryId to (SystemCategoryCatalog.categories.firstOrNull { category -> category.id == categoryId }?.name ?: it.name)
            }
        val avoidedCategoryHits = (categoryNamesById.keys intersect preferences.avoidedCategoryIds)
            .map { categoryNamesById.getValue(it) }
            .toSet() + preferences.avoidedCategoryIds.mapNotNull { categoryId ->
            val category = SystemCategoryCatalog.categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
            val categoryTerms = category.code?.avoidanceTerms().orEmpty()
            category.name.takeIf { categoryTerms.isNotEmpty() && terms.matchesAnyPreferenceTerm(categoryTerms) }
        }

        return SafetyResult(
            safe = allergyHits.isEmpty() && dietHits.isEmpty() && avoidedProductHits.isEmpty() && avoidedCategoryHits.isEmpty(),
            warnings = buildList {
                allergyHits.forEach { add("Исключено из-за аллергена: $it") }
                dietHits.forEach { add("Исключено из-за ограничения: ${it.name}") }
                avoidedProductHits.forEach { add("Исключено из-за продукта в списке «избегать»: $it") }
                avoidedCategoryHits.forEach { add("Исключено из-за категории в списке «избегать»: $it") }
                dislikedHits.forEach { add("Содержит нежелательный ингредиент: $it") }
            },
            dislikedIngredientMatches = dislikedHits.toSet(),
            dietaryRestrictionViolations = dietHits,
            allergyMatches = allergyHits.toSet(),
            avoidedProductMatches = avoidedProductHits,
            avoidedCategoryMatches = avoidedCategoryHits
        )
    }

}

data class SafetyResult(
    val safe: Boolean,
    val warnings: List<String>,
    val dislikedIngredientMatches: Set<String>,
    val dietaryRestrictionViolations: Set<DietaryRestriction>,
    val allergyMatches: Set<String>,
    val avoidedProductMatches: Set<String> = emptySet(),
    val avoidedCategoryMatches: Set<String> = emptySet()
)

fun ingredientTerms(value: String): Set<String> {
    val normalized = value.normalizeIngredient()
    return setOf(normalized) + SAFETY_ALIASES[normalized].orEmpty()
}

fun Set<String>.matchesAnyPreferenceTerm(needles: Set<String>): Boolean =
    any { term -> needles.any { needle -> term.contains(needle) || needle.contains(term) } }

fun String.normalizeIngredient(): String =
    trim()
        .lowercase(Locale.ROOT)
        .replace('ё', 'е')

private fun DietaryRestriction.violationTerms(): Set<String> =
    when (this) {
        DietaryRestriction.VEGETARIAN -> setOf(
            "meat",
            "pork",
            "beef",
            "chicken",
            "fish",
            "seafood",
            "мяс",
            "свинин",
            "говядин",
            "куриц",
            "рыб",
            "морепродукт"
        )
        DietaryRestriction.VEGAN -> setOf(
            "meat",
            "pork",
            "beef",
            "chicken",
            "fish",
            "seafood",
            "milk",
            "cheese",
            "yogurt",
            "egg",
            "honey",
            "мяс",
            "рыб",
            "молок",
            "сыр",
            "йогурт",
            "кефир",
            "яйц",
            "мед"
        )
        DietaryRestriction.GLUTEN_FREE -> setOf("wheat", "barley", "rye", "flour", "bread", "pasta", "пшениц", "ячмен", "рож", "мук", "хлеб", "макарон", "паст")
        DietaryRestriction.DAIRY_FREE -> setOf("milk", "cheese", "yogurt", "cream", "butter", "молок", "сыр", "йогурт", "кефир", "сливк", "масло")
        DietaryRestriction.NUT_FREE -> setOf("nut", "peanut", "almond", "cashew", "орех", "арахис", "миндаль", "кешью")
        DietaryRestriction.HALAL -> setOf("pork", "bacon", "ham", "alcohol", "wine", "свинин", "бекон", "ветчин", "алкогол", "вино")
        DietaryRestriction.KOSHER -> setOf("pork", "bacon", "ham", "shellfish", "shrimp", "свинин", "бекон", "ветчин", "кревет", "моллюск")
    }

private fun ProductCategory.avoidanceTerms(): Set<String> =
    when (this) {
        ProductCategory.DAIRY -> setOf("milk", "cheese", "yogurt", "cream", "butter", "dairy", "молок", "сыр", "йогурт", "кефир", "сливк", "масло", "творог")
        ProductCategory.MEAT_FISH -> setOf("meat", "pork", "beef", "chicken", "fish", "seafood", "shrimp", "мяс", "свинин", "говядин", "куриц", "рыб", "морепродукт", "кревет")
        ProductCategory.VEGETABLES_FRUITS -> setOf("vegetable", "fruit", "apple", "tomato", "potato", "овощ", "фрукт", "яблок", "томат", "картоф")
        ProductCategory.CEREALS -> setOf("cereal", "grain", "rice", "oat", "buckwheat", "flour", "bread", "pasta", "круп", "рис", "овсян", "греч", "мук", "хлеб", "макарон")
        ProductCategory.BEVERAGES -> setOf("drink", "beverage", "juice", "water", "tea", "coffee", "напит", "сок", "вод", "чай", "кофе")
        ProductCategory.OTHER -> emptySet()
    }

private val SAFETY_ALIASES = mapOf(
    "milk" to setOf("молок", "молоч"),
    "yogurt" to setOf("йогурт", "кефир", "ряженка"),
    "fish" to setOf("рыб", "лосос", "тунец", "треска", "форель", "семга"),
    "meat" to setOf("мяс", "говядин", "свинин", "куриц", "индейк", "фарш"),
    "nut" to setOf("орех", "арахис", "миндаль", "кешью"),
    "gluten" to setOf("пшениц", "ячмен", "рож", "мук", "хлеб", "макарон", "паст"),
    "dairy" to setOf("молок", "сыр", "йогурт", "кефир", "сливк", "творог"),
    "rice" to setOf("рис", "рисовая"),
    "cereal" to setOf("круп", "греч", "овсян", "рис", "макарон", "паст"),
    "vegetables" to setOf("овощ", "помидор", "томат", "огурец", "морков", "картоф", "капуст", "перец"),
    "fruit" to setOf("фрукт", "яблок", "банан", "груш", "ягод", "апельсин"),
    "beverage" to setOf("напит", "сок", "вод", "чай", "кофе"),
    "tomato" to setOf("томат", "помидор"),
    "tomatoes" to setOf("томат", "помидор")
)
