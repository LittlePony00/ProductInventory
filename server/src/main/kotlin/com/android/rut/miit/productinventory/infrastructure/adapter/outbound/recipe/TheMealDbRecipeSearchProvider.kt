package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class TheMealDbRecipeSearchProvider(
    restClientBuilder: RestClient.Builder,
    @param:Value("\${external-recipes.themealdb.enabled:true}") private val enabled: Boolean,
    @Value("\${external-recipes.themealdb.base-url:https://www.themealdb.com/api/json/v1/1}") baseUrl: String,
    @param:Value("\${external-recipes.themealdb.max-products:3}") private val maxProducts: Int,
    @param:Value("\${external-recipes.themealdb.max-recipes:4}") private val maxRecipes: Int
) : IExternalRecipeSearchProvider {

    private val log = LoggerFactory.getLogger(TheMealDbRecipeSearchProvider::class.java)
    private val sourceBaseUrl = baseUrl.trimEnd('/')
    private val restClient = restClientBuilder.baseUrl(baseUrl).build()

    override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
        if (!enabled || context.candidateProducts.isEmpty()) return emptyList()
        return context.candidateProducts
            .asSequence()
            .mapNotNull(Product::themealdbIngredient)
            .distinct()
            .take(maxProducts.coerceAtLeast(1))
            .flatMap(::findMealsByIngredient)
            .distinctBy(MealSummary::id)
            .shuffledCandidates()
            .take(maxRecipes.coerceAtLeast(1))
            .mapNotNull(::lookupMeal)
            .map { recipe ->
                RecipeDiscoveryResult(
                    recipe = recipe.toRecipe(),
                    source = RecipeSource.EXTERNAL_API,
                    sourceName = THE_MEAL_DB_SOURCE_NAME,
                    sourceUrl = "$sourceBaseUrl/lookup.php?i=${recipe.id}",
                    imageUrl = recipe.thumbnailUrl,
                    reasons = listOf("Рецепт найден во внешнем сервисе TheMealDB по текущим продуктам")
                )
            }
            .toList()
    }

    override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
        if (!enabled) return emptyList()
        return (1..maxRecipes.coerceAtLeast(1))
            .asSequence()
            .mapNotNull { lookupRandomMeal() }
            .distinctBy(MealDetails::id)
            .map { recipe ->
                RecipeDiscoveryResult(
                    recipe = recipe.toRecipe(),
                    source = RecipeSource.EXTERNAL_API,
                    sourceName = THE_MEAL_DB_SOURCE_NAME,
                    sourceUrl = "$sourceBaseUrl/lookup.php?i=${recipe.id}",
                    imageUrl = recipe.thumbnailUrl,
                    reasons = listOf("Случайный рецепт найден во внешнем сервисе TheMealDB")
                )
            }
            .toList()
    }

    private fun findMealsByIngredient(ingredient: String): Sequence<MealSummary> =
        request("/filter.php?i={ingredient}", mapOf("ingredient" to ingredient))
            ?.path("meals")
            ?.takeIf(JsonNode::isArray)
            ?.asSequence()
            ?.mapNotNull { meal ->
                val id = meal.path("idMeal").textOrNull()
                val title = meal.path("strMeal").textOrNull()
                if (id == null || title == null) null else MealSummary(id = id, title = title)
            }
            .orEmpty()

    private fun lookupMeal(summary: MealSummary): MealDetails? =
        request("/lookup.php?i={id}", mapOf("id" to summary.id))
            ?.path("meals")
            ?.takeIf(JsonNode::isArray)
            ?.firstOrNull()
            ?.toMealDetails(fallbackId = summary.id, fallbackTitle = summary.title)
            ?.takeIf { it.ingredients.isNotEmpty() && !it.instructions.isNullOrBlank() }

    private fun lookupRandomMeal(): MealDetails? =
        request("/random.php")
            ?.path("meals")
            ?.takeIf(JsonNode::isArray)
            ?.firstOrNull()
            ?.toMealDetails()
            ?.takeIf { it.ingredients.isNotEmpty() && !it.instructions.isNullOrBlank() }

    private fun request(path: String, variables: Map<String, String> = emptyMap()): JsonNode? =
        try {
            restClient.get()
                .uri(path, variables)
                .retrieve()
                .body(JsonNode::class.java)
        } catch (exception: RestClientException) {
            log.warn("TheMealDB recipe search failed: {}", exception.message)
            null
        }
}

private data class MealSummary(
    val id: String,
    val title: String
)

private fun Sequence<MealSummary>.shuffledCandidates(): Sequence<MealSummary> =
    toList().shuffled().asSequence()

private data class MealDetails(
    val id: String,
    val title: String,
    val instructions: String?,
    val thumbnailUrl: String?,
    val ingredients: List<RecipeIngredient>
) {
    fun toRecipe(): Recipe =
        Recipe(
            title = title,
            ingredients = ingredients,
            steps = instructions
                ?.split(stepSeparator)
                ?.map(String::trim)
                ?.filter(String::isNotBlank)
                ?.takeIf(List<String>::isNotEmpty)
                ?: listOf("Следуйте инструкции источника."),
            time = "Время зависит от рецепта",
            calories = 0,
            caloriesKnown = false
        )
}

private fun JsonNode.ingredients(): List<RecipeIngredient> =
    (1..20).mapNotNull { index ->
        val name = path("strIngredient$index").textOrNull()
        val measure = path("strMeasure$index").textOrNull()
        if (name == null) null else RecipeIngredient(name = name, amount = measure ?: "по вкусу")
    }

private fun JsonNode.toMealDetails(
    fallbackId: String? = null,
    fallbackTitle: String? = null
): MealDetails? {
    val id = fallbackId ?: path("idMeal").textOrNull() ?: return null
    val title = path("strMeal").textOrNull() ?: fallbackTitle ?: return null
    return MealDetails(
        id = id,
        title = title,
        instructions = path("strInstructions").textOrNull(),
        thumbnailUrl = path("strMealThumb").textOrNull(),
        ingredients = ingredients()
    )
}

private fun JsonNode.textOrNull(): String? =
    takeUnless { it.isMissingNode || it.isNull }
        ?.asText()
        ?.trim()
        ?.takeIf(String::isNotBlank)

private fun Product.themealdbIngredient(): String? {
    val normalized = name.trim().lowercase().replace('ё', 'е')
    return themealdbIngredientAliases[normalized]
        ?: normalized.takeIf { it.all { char -> char in 'a'..'z' || char == ' ' || char == '-' } }
}

private val themealdbIngredientAliases = mapOf(
    "рис" to "rice",
    "молоко" to "milk",
    "курица" to "chicken",
    "куриное филе" to "chicken",
    "говядина" to "beef",
    "свинина" to "pork",
    "рыба" to "fish",
    "картофель" to "potatoes",
    "картошка" to "potatoes",
    "томат" to "tomato",
    "томаты" to "tomato",
    "помидор" to "tomato",
    "помидоры" to "tomato",
    "яйцо" to "eggs",
    "яйца" to "eggs",
    "сыр" to "cheese",
    "лук" to "onion",
    "морковь" to "carrots",
    "паста" to "pasta",
    "макароны" to "pasta"
)

private const val THE_MEAL_DB_SOURCE_NAME = "TheMealDB"
private val stepSeparator = Regex("""\r?\n+""")
