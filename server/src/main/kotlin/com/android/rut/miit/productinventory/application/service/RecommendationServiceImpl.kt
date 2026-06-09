package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSafetyFilter
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSearchScope
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContextBuilder
import com.android.rut.miit.productinventory.application.service.recommendation.cookingTimeMinutes
import com.android.rut.miit.productinventory.application.service.recommendation.ingredientTerms
import com.android.rut.miit.productinventory.application.service.recommendation.matchesAnyPreferenceTerm
import com.android.rut.miit.productinventory.domain.exception.DomainException
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeIngredientOption
import com.android.rut.miit.productinventory.domain.model.RecipeRecommendation
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.port.inbound.IRecommendationService
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeGenerator
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeLocalizer
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.util.UUID

@Service
class RecommendationServiceImpl(
    private val contextBuilder: RecommendationContextBuilder,
    private val safetyFilter: RecipeSafetyFilter,
    private val aiRecipeGenerator: IAiRecipeGenerator,
    private val externalRecipeSearchProviders: List<IExternalRecipeSearchProvider> = emptyList(),
    private val aiRecipeSearchProvider: IAiRecipeSearchProvider? = null,
    private val aiRecipeLocalizer: IAiRecipeLocalizer? = null
) : IRecommendationService {

    @Transactional(readOnly = true)
    override fun getRecipes(
        userId: UUID,
        householdId: UUID,
        mode: RecommendationMode
    ): List<RecipeRecommendation> {
        require(mode != RecommendationMode.AI_GENERATED_CUSTOM) {
            "AI_GENERATED_CUSTOM нужно запрашивать через отдельный метод генерации ИИ-рецепта"
        }
        val context = contextBuilder.build(userId, householdId, mode)
        return buildExternalRecommendations(context)
            .sortedWith(compareByDescending<RecipeRecommendation> { it.score }.thenBy { it.title })
            .take(RECOMMENDATION_LIMIT)
    }

    @Transactional(readOnly = true)
    override fun getIngredientOptions(userId: UUID, householdId: UUID): List<RecipeIngredientOption> {
        val context = contextBuilder.build(userId, householdId, RecommendationMode.CURRENT_PRODUCTS)
        return context.allowedProducts.map { it.toIngredientOption() }
    }

    @Transactional(readOnly = true)
    override fun findRecipes(
        userId: UUID,
        householdId: UUID,
        request: RecipeSearchRequest
    ): List<RecipeRecommendation> {
        val context = contextBuilder.build(
            userId = userId,
            householdId = householdId,
            mode = RecommendationMode.CURRENT_PRODUCTS,
            selectedProductIds = request.selectedProductIds,
            searchScope = if (request.selectedProductIds.isEmpty()) {
                // Empty selection is the explicit random flow: search any safe recipe, not all stocked products.
                RecipeSearchScope.ANY_PRODUCTS
            } else {
                RecipeSearchScope.STOCK_PRODUCTS
            }
        )
        val externalRecommendations = buildExternalRecommendations(context)
        val aiRecommendations = buildAiAssistedWebRecommendations(context)
        val recommendationLimit = context.recommendationLimit()
        val foundRecommendations = (
            externalRecommendations +
                aiRecommendations
            )
            .distinctBy { it.title.lowercase() }
            .selectDiverseFor(context, recommendationLimit)

        if (foundRecommendations.isNotEmpty()) {
            return foundRecommendations
        }

        return buildCreatedRecipeRecommendation(context)?.let(::listOf).orEmpty()
    }

    private fun buildExternalRecommendations(context: RecommendationContext): List<RecipeRecommendation> {
        val results = externalRecipeSearchProviders
            .flatMap { provider ->
                if (context.searchesAnyProducts) {
                    provider.searchRandomRecipes(context)
                } else {
                    provider.searchRecipes(context)
                }
            }
        return localizeExternalRecipes(results)
            .mapNotNull { result -> result.toRecommendation(context) }
    }

    private fun buildAiAssistedWebRecommendations(context: RecommendationContext): List<RecipeRecommendation> =
        aiRecipeSearchProvider
            ?.searchWebRecipes(context)
            ?.mapNotNull { result -> result.toRecommendation(context) }
            .orEmpty()

    private fun localizeExternalRecipe(result: RecipeDiscoveryResult): RecipeDiscoveryResult? {
        if (result.source != RecipeSource.EXTERNAL_API) return result
        return runCatching {
            aiRecipeLocalizer?.localizeAndEnrichFoundRecipe(result.recipe)
        }
            .getOrNull()
            ?.let { result.copy(recipe = it) }
    }

    private fun localizeExternalRecipes(results: List<RecipeDiscoveryResult>): List<RecipeDiscoveryResult> {
        if (results.isEmpty()) return emptyList()
        val externalResults = results.filter { it.source == RecipeSource.EXTERNAL_API && it.requiresLocalization }
        if (externalResults.isEmpty()) return results
        val localizedByIndex = runCatching {
            aiRecipeLocalizer
                ?.localizeAndEnrichFoundRecipes(externalResults.map { it.recipe })
                .orEmpty()
        }.getOrDefault(emptyList())
        var externalIndex = 0
        return results.map { result ->
            if (result.source != RecipeSource.EXTERNAL_API || !result.requiresLocalization) {
                result
            } else {
                localizedByIndex
                    .getOrNull(externalIndex++)
                    ?.let { localized -> result.copy(recipe = localized) }
                    ?: result.copy(
                        warnings = result.warnings + "Не удалось автоматически перевести рецепт из внешнего источника; показан исходный текст."
                    )
            }
        }
    }

    private fun buildCreatedRecipeRecommendation(
        context: RecommendationContext
    ): RecipeRecommendation? {
        if (!context.searchesAnyProducts && context.candidateProducts.isEmpty()) return null
        val recipe = aiRecipeGenerator.generateRecipe(
            AiRecipeGenerationContext(
                products = context.candidateProducts,
                preferences = context.preferences,
                request = AiRecipeGenerationRequest(
                    extraNotes = context.generatedFallbackPrompt()
                ),
                allProducts = context.products
            )
        ) ?: return null
        val safetyResult = safetyFilter.evaluate(
            recipe = recipe,
            requiredIngredients = recipe.ingredients.map { it.name }.toSet(),
            products = context.candidateProducts,
            preferences = context.preferences
        )
        if (!safetyResult.safe) return null

        return RecipeRecommendation(
            title = recipe.title,
            ingredients = recipe.ingredients,
            steps = recipe.steps,
            time = recipe.time,
            cookingTimeMinutes = recipe.cookingTimeMinutes(),
            calories = recipe.calories,
            caloriesKnown = recipe.caloriesKnown,
            source = RecipeSource.AI_GENERATED,
            sourceName = GIGACHAT_SOURCE_NAME,
            usedHouseholdProducts = if (context.searchesAnyProducts) {
                emptyList()
            } else {
                context.candidateProducts.map { it.name }
            },
            usedExpiringProducts = if (context.searchesAnyProducts) {
                emptyList()
            } else {
                context.expiringProducts.map { it.name }
            },
            missingIngredients = emptyList(),
            reasons = listOf(context.generatedFallbackReason()),
            warnings = listOf(AI_WARNING) + safetyResult.warnings,
            aiAssisted = false,
            aiGenerated = true
        )
    }

    private fun RecommendationContext.recommendationLimit(): Int =
        RECOMMENDATION_LIMIT

    @Transactional(readOnly = true)
    override fun generateAiRecipe(
        userId: UUID,
        householdId: UUID,
        request: AiRecipeGenerationRequest
    ): RecipeRecommendation {
        val context = contextBuilder.build(userId, householdId, RecommendationMode.AI_GENERATED_CUSTOM)
        val preferences = context.preferences.copy(
            maxCookingTimeMinutes = request.maxCookingTimeMinutes ?: context.preferences.maxCookingTimeMinutes,
            servings = request.servings ?: context.preferences.servings
        )
        val recipe = aiRecipeGenerator.generateRecipe(
            AiRecipeGenerationContext(
                products = context.candidateProducts,
                preferences = preferences,
                request = request,
                allProducts = context.products
            )
        ) ?: throw AiRecipeUnavailableException()
        val safetyResult = safetyFilter.evaluate(
            recipe = recipe,
            requiredIngredients = recipe.ingredients.map { it.name }.toSet(),
            products = context.candidateProducts,
            preferences = preferences
        )
        require(safetyResult.safe) {
            "ИИ-рецепт отклонён детерминированной проверкой безопасности"
        }

        return RecipeRecommendation(
            title = recipe.title,
            ingredients = recipe.ingredients,
            steps = recipe.steps,
            time = recipe.time,
            cookingTimeMinutes = recipe.cookingTimeMinutes(),
            calories = recipe.calories,
            caloriesKnown = recipe.caloriesKnown,
            source = RecipeSource.AI_GENERATED,
            sourceName = GIGACHAT_SOURCE_NAME,
            score = 0.0,
            usedHouseholdProducts = context.products.map { it.name },
            usedExpiringProducts = context.expiringProducts.map { it.name },
            missingIngredients = emptyList(),
            reasons = listOf("Рецепт создан ИИ по текущим продуктам и сохранённым предпочтениям"),
            warnings = listOf(AI_WARNING) + safetyResult.warnings,
            aiGenerated = true
        )
    }

    private companion object {
        const val RECOMMENDATION_LIMIT = 6
        const val AI_WARNING = "Рецепт создан ИИ. Проверьте ингредиенты, аллергены и шаги приготовления перед использованием."
        const val GIGACHAT_SOURCE_NAME = "GigaChat"
    }
}

class AiRecipeUnavailableException :
    DomainException("Генерация ИИ-рецепта недоступна")

private fun List<RecipeRecommendation>.rankFor(context: RecommendationContext): List<RecipeRecommendation> =
    if (context.searchesAnyProducts) {
        listOf(
            filter { it.source == RecipeSource.EXTERNAL_API }.roundRobinByProvider(),
            filter { it.aiAssisted }.shuffled(),
            filter { it.source != RecipeSource.EXTERNAL_API && !it.aiAssisted }.shuffled()
        ).roundRobin()
    } else {
        sortedWith(compareByDescending<RecipeRecommendation> { it.score }.thenBy { it.title })
            .interleaveExternalProviders()
    }

private fun List<List<RecipeRecommendation>>.roundRobin(): List<RecipeRecommendation> {
    val remaining = map { it.toMutableList() }
    val result = mutableListOf<RecipeRecommendation>()
    while (remaining.any { it.isNotEmpty() }) {
        remaining.forEach { bucket ->
            bucket.removeFirstOrNull()?.let(result::add)
        }
    }
    return result
}

private fun List<RecipeRecommendation>.selectDiverseFor(
    context: RecommendationContext,
    limit: Int
): List<RecipeRecommendation> {
    val ranked = distinctBy { it.title.lowercase() }.rankFor(context)
    return ranked
        .take(limit)
        .ensureSourceIncluded(ranked, limit) { it.source == RecipeSource.EXTERNAL_API }
        .ensureSourceIncluded(ranked, limit) { it.aiAssisted }
        .ensureExternalProviderIncluded(ranked, limit)
}

private fun List<RecipeRecommendation>.ensureSourceIncluded(
    ranked: List<RecipeRecommendation>,
    limit: Int,
    predicate: (RecipeRecommendation) -> Boolean
): List<RecipeRecommendation> {
    if (any(predicate) || ranked.none(predicate) || size < limit) return this
    val replacement = ranked.first(predicate)
    val replacementSourcePresent = any { it.source == replacement.source && it.aiAssisted == replacement.aiAssisted }
    if (replacementSourcePresent) return this
    val indexToReplace = indexOfLast { candidate ->
        candidate.source != RecipeSource.EXTERNAL_API && !candidate.aiAssisted
    }.takeIf { it >= 0 } ?: lastIndex
    return mapIndexed { index, candidate ->
        if (index == indexToReplace) replacement else candidate
    }
}

private fun List<RecipeRecommendation>.ensureExternalProviderIncluded(
    ranked: List<RecipeRecommendation>,
    limit: Int
): List<RecipeRecommendation> {
    val rankedProviderKeys = ranked
        .filter { it.source == RecipeSource.EXTERNAL_API }
        .map(RecipeRecommendation::externalProviderKey)
        .distinct()
    val selectedProviderKeys = filter { it.source == RecipeSource.EXTERNAL_API }
        .map(RecipeRecommendation::externalProviderKey)
        .toSet()
    val missingProviderKey = rankedProviderKeys.firstOrNull { it !in selectedProviderKeys } ?: return this
    val replacement = ranked.firstOrNull {
        it.source == RecipeSource.EXTERNAL_API && it.externalProviderKey() == missingProviderKey
    } ?: return this

    if (size < limit) return this + replacement

    val duplicatedExternalProviderKeys = filter { it.source == RecipeSource.EXTERNAL_API }
        .groupingBy(RecipeRecommendation::externalProviderKey)
        .eachCount()
        .filterValues { it > 1 }
        .keys
    val indexToReplace = indexOfLast { candidate ->
        candidate.source == RecipeSource.EXTERNAL_API && candidate.externalProviderKey() in duplicatedExternalProviderKeys
    }.takeIf { it >= 0 } ?: indexOfLast { candidate ->
        candidate.source != RecipeSource.EXTERNAL_API && !candidate.aiAssisted
    }.takeIf { it >= 0 } ?: return this

    return mapIndexed { index, candidate ->
        if (index == indexToReplace) replacement else candidate
    }
}

private fun List<RecipeRecommendation>.interleaveExternalProviders(): List<RecipeRecommendation> {
    val interleavedExternal = filter { it.source == RecipeSource.EXTERNAL_API }
        .roundRobinByProvider()
        .iterator()
    return map { candidate ->
        if (candidate.source == RecipeSource.EXTERNAL_API && interleavedExternal.hasNext()) {
            interleavedExternal.next()
        } else {
            candidate
        }
    }
}

private fun List<RecipeRecommendation>.roundRobinByProvider(): List<RecipeRecommendation> =
    groupBy(RecipeRecommendation::externalProviderKey)
        .values
        .map { providerRecipes -> providerRecipes.shuffled() }
        .toList()
        .roundRobin()

private fun RecipeRecommendation.externalProviderKey(): String =
    sourceName
        ?.trim()
        ?.lowercase()
        ?.takeIf(String::isNotBlank)
        ?: sourceUrl
        ?.let { url -> runCatching { URI.create(url).host?.lowercase() }.getOrNull() }
        ?.removePrefix("www.")
        ?: sourceUrl
        ?: source.name

private fun RecommendationContext.generatedFallbackPrompt(): String =
    if (searchesAnyProducts) {
        "Внешние сервисы и поиск рецептов на сайтах не нашли подходящий случайный рецепт. Создай новый случайный рецепт без привязки к текущим продуктам, но строго с учётом сохранённых предпочтений, аллергий и ограничений."
    } else {
        "Внешние сервисы и поиск рецептов на сайтах не нашли подходящий рецепт. Создай новый рецепт по выбранным продуктам."
    }

private fun RecommendationContext.generatedFallbackReason(): String =
    if (searchesAnyProducts) {
        "Рецепт создан ИИ, потому что внешние сервисы и поиск на сайтах не нашли безопасный случайный вариант"
    } else {
        "Рецепт создан ИИ, потому что внешние сервисы и поиск на сайтах не нашли подходящий вариант"
    }

private fun Product.toIngredientOption(): RecipeIngredientOption =
    RecipeIngredientOption(
        id = id,
        name = name,
        categoryName = categoryName ?: category.name,
        remainingAmount = remainingAmount,
        unit = quantity.unit,
        expiring = expirationDate?.status in setOf(
            ExpirationStatus.EXPIRED,
            ExpirationStatus.EXPIRING_SOON
        )
    )

private fun RecipeDiscoveryResult.toRecommendation(context: RecommendationContext): RecipeRecommendation? {
    val safetyFilter = RecipeSafetyFilter()
    val safetyResult = safetyFilter.evaluate(
        recipe = recipe,
        requiredIngredients = recipe.ingredients.map { it.name }.toSet(),
        products = context.candidateProducts,
        preferences = context.preferences
    )
    if (!safetyResult.safe) return null
    val matchableProducts = context.productsForIngredientMatching()
    val usedProducts = recipe.usedProducts(matchableProducts)
    val missingIngredients = recipe.ingredients
        .filterNot { ingredient ->
            usedProducts.any { product ->
                ingredientTerms(ingredient.name).matchesAnyPreferenceTerm(product.recipeTerms())
            }
        }
        .map { it.name }
        .distinct()

    return RecipeRecommendation(
        title = recipe.title,
        ingredients = recipe.ingredients,
        steps = recipe.steps,
        time = recipe.time,
        cookingTimeMinutes = recipe.cookingTimeMinutes(),
        calories = recipe.calories,
        caloriesKnown = recipe.caloriesKnown,
        source = source,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        imageUrl = imageUrl,
        score = source.defaultScore(),
        usedHouseholdProducts = usedProducts.map(Product::name),
        usedExpiringProducts = usedProducts
            .filter { it.expirationDate?.status in setOf(ExpirationStatus.EXPIRED, ExpirationStatus.EXPIRING_SOON) }
            .map(Product::name),
        missingIngredients = missingIngredients,
        reasons = reasons,
        warnings = warnings + safetyResult.warnings,
        aiAssisted = aiAssisted,
        aiGenerated = false
    )
}

private fun RecommendationContext.productsForIngredientMatching(): List<Product> =
    if (searchesAnyProducts) {
        allowedProducts
    } else {
        candidateProducts
    }

private fun Recipe.usedProducts(products: List<Product>): List<Product> {
    val recipeTerms = ingredients.flatMap { ingredientTerms(it.name) }.toSet()
    return products
        .filter { product -> product.recipeTerms().matchesAnyPreferenceTerm(recipeTerms) }
        .distinctBy(Product::id)
}

private fun Product.recipeTerms(): Set<String> =
    (listOf(name, category.name) + listOfNotNull(categoryName, ingredientsText))
        .flatMap(::ingredientTerms)
        .toSet()

private fun RecipeSource.defaultScore(): Double =
    when (this) {
        RecipeSource.LOCAL_KNOWLEDGE_BASE -> 100.0
        RecipeSource.EXTERNAL_API -> 80.0
        RecipeSource.AI_ASSISTED -> 70.0
        RecipeSource.AI_GENERATED -> 0.0
    }
