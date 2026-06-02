package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

@Component
@Order(0)
class PovarRuRecipeSearchProvider(
    restClientBuilder: RestClient.Builder,
    @param:Value("\${external-recipes.povar.enabled:true}") private val enabled: Boolean,
    @Value("\${external-recipes.povar.base-url:https://povar.ru}") baseUrl: String,
    @param:Value("\${external-recipes.povar.max-products:2}") private val maxProducts: Int,
    @param:Value("\${external-recipes.povar.max-recipes:3}") private val maxRecipes: Int
) : IExternalRecipeSearchProvider {

    private val log = LoggerFactory.getLogger(PovarRuRecipeSearchProvider::class.java)
    private val sourceBaseUrl = baseUrl.trimEnd('/')
    private val restClient = restClientBuilder.baseUrl(sourceBaseUrl).build()
    private val randomTargetOffset = AtomicInteger(0)

    override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
        if (!enabled || context.candidateProducts.isEmpty()) return emptyList()
        return context.candidateProducts
            .asSequence()
            .take(maxProducts.coerceAtLeast(1))
            .flatMap(Product::povarSearchTargets)
            .distinct()
            .flatMap(::findRecipeSummariesByTarget)
            .distinctBy(PovarRecipeSummary::url)
            .take(maxRecipes.coerceAtLeast(1) * LOOKUP_CANDIDATE_MULTIPLIER)
            .mapNotNull(::lookupRecipe)
            .take(maxRecipes.coerceAtLeast(1))
            .map { recipe ->
                recipe.toDiscoveryResult(
                    "Рецепт найден в русскоязычном внешнем источнике Povar.ru по выбранным продуктам"
                )
            }
            .toList()
    }

    override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
        if (!enabled) return emptyList()
        return povarRandomSearchTargets
            .rotatedBy(randomTargetOffset.getAndIncrement())
            .asSequence()
            .flatMap(::findRecipeSummariesByTarget)
            .distinctBy(PovarRecipeSummary::url)
            .take(maxRecipes.coerceAtLeast(1) * LOOKUP_CANDIDATE_MULTIPLIER)
            .mapNotNull(::lookupRecipe)
            .take(maxRecipes.coerceAtLeast(1))
            .map { recipe ->
                recipe.toDiscoveryResult("Случайный рецепт найден в русскоязычном внешнем источнике Povar.ru")
            }
            .toList()
    }

    private fun findRecipeSummariesByTarget(target: PovarSearchTarget): Sequence<PovarRecipeSummary> {
        val document = requestDocument(target.path) ?: return emptySequence()
        return document.select(".recipe_list .recipe").asSequence()
            .mapNotNull { element ->
                val titleElement = element.selectFirst("a.listRecipieTitle") ?: return@mapNotNull null
                val path = titleElement.attr("href").takeIf(String::isNotBlank) ?: return@mapNotNull null
                val title = titleElement.text().cleanText().takeIf(String::isNotBlank) ?: return@mapNotNull null
                val imageUrl = element.selectFirst(".thumb img")?.absUrl("src")?.takeIf(String::isNotBlank)
                PovarRecipeSummary(title = title, url = absoluteUrl(path), imageUrl = imageUrl)
            }
    }

    private fun lookupRecipe(summary: PovarRecipeSummary): PovarRecipeDetails? {
        val document = requestDocument(URI.create(summary.url)) ?: return null
        val title = document.selectFirst("[itemprop=name].detailed, h1.detailed")
            ?.text()
            ?.cleanText()
            ?.takeIf(String::isNotBlank)
            ?: summary.title
        val ingredients = document.select("[itemprop=recipeIngredient]")
            .mapNotNull(Element::toIngredient)
            .distinctBy { it.name.lowercase() }
        val steps = document.select("[itemprop=recipeInstructions] .detailed_step_description_big")
            .mapNotNull { it.text().cleanStep() }
            .ifEmpty {
                document.select("[itemprop=recipeInstructions]")
                    .flatMap { it.html().split(stepBreakRegex) }
                    .mapNotNull { it.cleanStep() }
            }
        if (ingredients.isEmpty() || steps.isEmpty()) return null
        return PovarRecipeDetails(
            title = title,
            url = summary.url,
            imageUrl = document.selectFirst(".bigImgBox [itemprop=image], [itemprop=image]")
                ?.absUrl("src")
                ?.takeIf(String::isNotBlank)
                ?: summary.imageUrl,
            ingredients = ingredients,
            steps = steps,
            time = document.selectFirst(".duration")?.text()?.cleanText()?.takeIf(String::isNotBlank)
                ?: document.selectFirst("[itemprop=totalTime], [itemprop=cookTime]")?.attr("content")?.toDisplayTime()
                ?: "Время зависит от рецепта",
            calories = document.selectFirst("[itemprop=calories]")
                ?.text()
                ?.let(calorieRegex::find)
                ?.value
                ?.toIntOrNull()
                ?: estimateCalories(ingredients.size)
        )
    }

    private fun requestDocument(path: String): Document? =
        requestDocument(URI.create(path))

    private fun requestDocument(uri: URI): Document? =
        try {
            val html = restClient.get()
                .uri(uri)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .retrieve()
                .body(String::class.java)
                ?: return null
            Jsoup.parse(html, sourceBaseUrl)
        } catch (exception: IllegalArgumentException) {
            log.warn("Povar.ru recipe URL is invalid: {}", exception.message)
            null
        } catch (exception: RestClientException) {
            log.warn("Povar.ru recipe search failed: {}", exception.message)
            null
        }

    private fun absoluteUrl(path: String): String =
        UriComponentsBuilder.fromUriString(sourceBaseUrl)
            .replacePath(path.substringBefore('#'))
            .replaceQuery(null)
            .build()
            .toUriString()
}

private data class PovarRecipeSummary(
    val title: String,
    val url: String,
    val imageUrl: String?
)

private data class PovarRecipeDetails(
    val title: String,
    val url: String,
    val imageUrl: String?,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val calories: Int
) {
    fun toRecipe(): Recipe =
        Recipe(
            title = title,
            ingredients = ingredients,
            steps = steps,
            time = time,
            calories = calories,
            caloriesKnown = true
        )
}

private fun PovarRecipeDetails.toDiscoveryResult(reason: String): RecipeDiscoveryResult =
    RecipeDiscoveryResult(
        recipe = toRecipe(),
        source = RecipeSource.EXTERNAL_API,
        sourceUrl = url,
        imageUrl = imageUrl,
        reasons = listOf(reason),
        requiresLocalization = false
    )

private fun Element.toIngredient(): RecipeIngredient? {
    val name = selectFirst(".name")?.text()?.cleanText()
        ?: attr("rel").cleanText().takeIf(String::isNotBlank)
        ?: ownText().cleanText().takeIf(String::isNotBlank)
        ?: return null
    val amount = listOf(
        selectFirst(".value")?.text(),
        selectFirst(".u-unit-name, .unit-name")?.text(),
        selectFirst(".descr")?.text()
    )
        .mapNotNull { it?.cleanText()?.takeIf(String::isNotBlank) }
        .joinToString(" ")
        .takeIf(String::isNotBlank)
        ?: "по вкусу"
    return RecipeIngredient(name = name, amount = amount)
}

private fun String.cleanText(): String =
    Jsoup.parse(this).text()
        .replace(nonBreakingSpaceRegex, " ")
        .replace(whitespaceRegex, " ")
        .trim()

private fun String.cleanStep(): String? =
    cleanText()
        .replace(stepNumberPrefixRegex, "")
        .trim()
        .takeIf { it.length >= MIN_STEP_LENGTH }

private fun String.toDisplayTime(): String? {
    val minutes = isoMinutesRegex.find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()
    return minutes?.let { "$it мин" }
}

private sealed interface PovarSearchTarget {
    val path: URI
}

private data class PovarCategoryTarget(
    val slug: String
) : PovarSearchTarget {
    override val path: URI = URI.create("/list/$slug/")
}

private data class PovarQueryTarget(
    val query: String
) : PovarSearchTarget {
    override val path: URI = UriComponentsBuilder.fromPath("/xmlsearch")
        .queryParam("query", query)
        .build()
        .encode()
        .toUri()
}

private fun Product.povarSearchTargets(): Sequence<PovarSearchTarget> {
    val normalized = name.trim().lowercase().replace('ё', 'е')
    return sequenceOf(
        povarCategoryAliases[normalized]?.let(::PovarCategoryTarget),
        normalized.takeIf { it.length >= MIN_QUERY_LENGTH }?.let(::PovarQueryTarget)
    ).filterNotNull()
}

private fun estimateCalories(ingredientCount: Int): Int =
    (ingredientCount * 90).coerceIn(120, 900)

private fun <T> List<T>.rotatedBy(offset: Int): List<T> {
    if (isEmpty()) return this
    val normalizedOffset = Math.floorMod(offset, size)
    return drop(normalizedOffset) + take(normalizedOffset)
}

private const val USER_AGENT = "ProductInventory/1.0 (+https://github.com/LittlePony00/ProductInventory)"
private const val MIN_STEP_LENGTH = 8
private const val MIN_QUERY_LENGTH = 2
private const val LOOKUP_CANDIDATE_MULTIPLIER = 3

private val povarCategoryAliases = mapOf(
    "курица" to "kurica",
    "куриное филе" to "kurinoe_file",
    "куриная грудка" to "kurinaya_grudka",
    "рис" to "ris",
    "картофель" to "kartofel",
    "картошка" to "kartofel",
    "молоко" to "moloko",
    "говядина" to "govyadina",
    "свинина" to "svinina",
    "рыба" to "ryba",
    "сыр" to "syr",
    "яйцо" to "yaica",
    "яйца" to "yaica",
    "помидор" to "pomidory",
    "помидоры" to "pomidory",
    "томат" to "pomidory",
    "томаты" to "pomidory",
    "лук" to "luk",
    "морковь" to "morkov",
    "макароны" to "makarony",
    "паста" to "pasta"
)

private val povarRandomSearchTargets: List<PovarSearchTarget> =
    listOf(
        "salad",
        "soup",
        "goryachie_bliuda",
        "vegies",
        "vypechka",
        "dessert"
    ).map(::PovarCategoryTarget)

private val whitespaceRegex = Regex("""\s+""")
private val nonBreakingSpaceRegex = Regex("""\u00a0""")
private val stepNumberPrefixRegex = Regex("""^\d+[\.\)]\s*""")
private val stepBreakRegex = Regex("""<br\s*/?>|\r?\n""", RegexOption.IGNORE_CASE)
private val calorieRegex = Regex("""\d+""")
private val isoMinutesRegex = Regex("""PT(\d+)M""")
