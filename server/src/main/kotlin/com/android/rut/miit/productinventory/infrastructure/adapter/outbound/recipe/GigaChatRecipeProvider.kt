package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.model.preferenceCategoryId
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeGenerator
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeLocalizer
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.AiRateLimiter
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.GigaChatAccessTokenProvider
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

@Component
class GigaChatRecipeProvider(
    restClientBuilder: RestClient.Builder,
    private val objectMapper: ObjectMapper,
    private val rateLimiter: AiRateLimiter,
    private val accessTokenProvider: GigaChatAccessTokenProvider,
    @Value("\${gigachat.base-url:}") baseUrl: String,
    @param:Value("\${gigachat.retry-attempts:2}") private val retryAttempts: Int,
    @param:Value("\${gigachat.retry-backoff-ms:250}") private val retryBackoffMs: Long
) : IRecipeProvider, IAiRecipeGenerator, IAiRecipeSearchProvider, IAiRecipeLocalizer {

    private val log = LoggerFactory.getLogger(GigaChatRecipeProvider::class.java)
    private val restClient = baseUrl.takeIf(String::isNotBlank)?.let { restClientBuilder.baseUrl(it).build() }

    override fun findRecipes(request: RecipeGenerationRequest): List<Recipe> {
        return requestRecipe(buildLegacyProviderPrompt(request.products))
            ?.let(::listOf)
            .orEmpty()
    }

    override fun generateRecipe(context: AiRecipeGenerationContext): Recipe? {
        return requestRecipe(buildCustomPrompt(context))
    }

    override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe? {
        return requestRecipe(buildFoundRecipeLocalizationPrompt(recipe))
            ?.mergeLocalizedFoundRecipe(original = recipe)
            ?: requestRecipe(buildFoundRecipeLocalizationRepairPrompt(recipe))
                ?.mergeLocalizedFoundRecipe(original = recipe)
    }

    override fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> {
        if (recipes.isEmpty()) return emptyList()
        if (recipes.size == 1) return listOf(localizeAndEnrichFoundRecipe(recipes.single()))
        val localizedRecipes = recipes.mergeLocalizedBatch(
            requestRecipes(buildFoundRecipesLocalizationPrompt(recipes))
        )
        if (localizedRecipes.none { it == null }) return localizedRecipes

        val failedOriginals = recipes.filterIndexed { index, _ -> localizedRecipes[index] == null }
        val repairedRecipes = failedOriginals.mergeLocalizedBatch(
            requestRecipes(buildFoundRecipesLocalizationRepairPrompt(failedOriginals))
        )
        var repairedIndex = 0
        return localizedRecipes.map { localized ->
            localized ?: repairedRecipes.getOrNull(repairedIndex++)
        }
    }

    override fun searchWebRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
        if (!context.searchesAnyProducts && context.candidateProducts.isEmpty()) return emptyList()
        val recipeCount = if (context.searchesAnyProducts) WEB_RANDOM_RECIPE_COUNT else WEB_SELECTED_RECIPE_COUNT
        return requestRecipes(buildWebSearchPrompt(context, recipeCount))
            .distinctBy { recipe -> recipe.title.trim().lowercase() }
            .take(recipeCount)
            .map { recipe ->
                    RecipeDiscoveryResult(
                        recipe = recipe,
                        source = RecipeSource.AI_ASSISTED,
                        sourceName = GIGACHAT_SOURCE_NAME,
                        reasons = listOf("AI-Assisted: ИИ искал подходящие идеи рецептов на кулинарных сайтах с учётом сохранённых предпочтений"),
                        warnings = listOf("AI-Assisted: проверьте источник, ингредиенты и аллергены перед использованием."),
                        aiAssisted = true
                    )
            }
    }

    private fun requestRecipe(prompt: String): Recipe? =
        requestRecipes(prompt).firstOrNull()

    private fun requestRecipes(prompt: String): List<Recipe> {
        val client = restClient ?: run {
            log.info("GigaChat recipe generation skipped: base URL is not configured")
            return emptyList()
        }
        if (!rateLimiter.tryAcquire()) {
            log.warn("GigaChat recipe generation skipped: rate limit exceeded")
            return emptyList()
        }
        val accessToken = accessTokenProvider.getAccessToken() ?: run {
            log.warn("GigaChat recipe generation skipped: access token is unavailable")
            return emptyList()
        }

        return retrying {
            val response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer $accessToken")
                .body(GigaChatRecipeRequest.from(prompt))
                .retrieve()
                .body(JsonNode::class.java)
                ?: return@retrying emptyList()

            val recipes = parseRecipes(response)
            if (recipes.isEmpty()) {
                log.warn("GigaChat recipe generation returned invalid recipe JSON")
            } else {
                log.info("GigaChat recipe generation succeeded with {} recipe(s)", recipes.size)
            }
            recipes
        }.orEmpty()
    }

    private fun <T> retrying(block: () -> T?): T? {
        repeat(retryAttempts.coerceAtLeast(1)) { attempt ->
            try {
                return block()
            } catch (exception: RestClientException) {
                if (exception is RestClientResponseException && exception.statusCode.value() == 429) {
                    log.warn("GigaChat recipe generation skipped: rate limit exceeded by upstream")
                    return null
                }
                if (attempt == retryAttempts.coerceAtLeast(1) - 1) {
                    log.warn("GigaChat recipe generation failed: {}", exception.message)
                    return null
                }
                Thread.sleep(retryBackoffMs.coerceAtLeast(0))
            } catch (exception: RuntimeException) {
                log.warn("GigaChat recipe response is not valid JSON recipe: {}", exception.message)
                return null
            }
        }
        return null
    }

    private fun parseRecipes(response: JsonNode): List<Recipe> {
        val content = response.at("/choices/0/message/content")
            .takeUnless(JsonNode::isMissingNode)
            ?.asText()
        val recipeNode = content
            ?.let { objectMapper.readTree(it.extractJsonPayload()) }
            ?: response

        return recipeNode.toRecipeList()
    }

    private fun buildLegacyProviderPrompt(products: List<Product>): String =
        """
        Верни только валидный JSON ровно с полями: title, ingredients, steps, time, calories.
        calories должен быть целым неотрицательным числом; если калорийность неизвестна, верни 0.
        title, ingredients.name, ingredients.amount, steps и time должны быть на русском языке.
        ingredients должен быть массивом объектов с name и amount. steps должен быть массивом строк.
        Создай новый рецепт по доступным запасам без использования локальной базы шаблонных рецептов.
        Сначала используй продукты из доступных запасов. Отдавай приоритет продуктам с близким сроком годности, остаткам и категориям.

        Запасы:
        ${products.joinToString(separator = "\n") { it.toPromptLine() }}
        """.trimIndent()

    private companion object {
        const val WEB_RANDOM_RECIPE_COUNT = 6
        const val WEB_SELECTED_RECIPE_COUNT = 3
        const val GIGACHAT_SOURCE_NAME = "GigaChat"
    }
}

private fun Product.toPromptLine(): String =
    "- $name; category=$category; remaining=$remainingAmount ${quantity.unit}; expires=${expirationDate?.date ?: "unknown"}"

private fun buildCustomPrompt(context: AiRecipeGenerationContext): String =
    """
    Верни только валидный JSON ровно с полями: title, ingredients, steps, time, calories.
    calories должен быть целым неотрицательным числом; если калорийность неизвестна, верни 0.
    title, ingredients.name, ingredients.amount, steps и time должны быть на русском языке.
    ingredients должен быть массивом объектов с name и amount. steps должен быть массивом строк.
    ${context.customRecipeProductInstruction()}
    Нельзя использовать аллергены и ингредиенты, нарушающие ограничения питания.

    Запасы:
    ${context.products.toPromptStockBlock()}

    Предпочтения:
    ${context.preferences.toPromptBlock(context.allProducts)}

    Дополнительные пожелания:
    ${context.request.extraNotes?.take(500)?.ifBlank { "нет" } ?: "нет"}
    """.trimIndent()

private fun buildWebSearchPrompt(context: RecommendationContext, recipeCount: Int): String =
    """
    Верни только валидный JSON ${if (recipeCount > 1) "массивом из $recipeCount разных рецептов" else "ровно с полями: title, ingredients, steps, time, calories"}.
    calories должен быть целым неотрицательным числом; если калорийность неизвестна, верни 0.
    title, ingredients.name, ingredients.amount, steps и time должны быть на русском языке.
    Каждый рецепт должен иметь ровно поля: title, ingredients, steps, time, calories.
    ingredients должен быть массивом объектов с name и amount. steps должен быть массивом строк.
    Найди на открытых кулинарных сайтах существующие идеи рецептов, которые подходят под ограничение по продуктам и сохранённые предпочтения.
    Рецепты должны отличаться друг от друга по названию, основным ингредиентам и способу приготовления.
    Не создавай полностью новый авторский рецепт на этом шаге. Нельзя использовать аллергены, запрещённые продукты и ингредиенты, нарушающие ограничения питания.
    variation_seed=${UUID.randomUUID()}

    Ограничение по продуктам:
    ${context.recipeSearchPromptScope()}

    Все продукты хозяйства:
    ${context.products.toPromptStockBlock()}

    Предпочтения:
    ${context.preferences.toPromptBlock(context.products)}
    """.trimIndent()

private fun buildFoundRecipeLocalizationPrompt(recipe: Recipe): String =
    """
    Верни только валидный JSON ровно с полями: title, ingredients, steps, time, calories.
    Переведи существующий рецепт на русский язык без изменения смысла.
    Нельзя добавлять, удалять, объединять, разделять или переставлять ингредиенты и шаги.
    ingredients должен содержать ровно ${recipe.ingredients.size} объектов в исходном порядке.
    steps должен содержать ровно ${recipe.steps.size} строк в исходном порядке.
    В title, ingredients.name, steps и time не должно остаться английских инструкций или непереведённых английских слов.
    Если time отсутствует или явно неизвестен, оцени примерное время приготовления на русском языке; иначе только переведи исходное время.
    Если calories отсутствует или явно неизвестен, оцени примерную калорийность целым числом; иначе верни исходное число ${recipe.calories}.
    Не меняй известную калорийность, если caloriesKnown=true.

    caloriesKnown=${recipe.caloriesKnown}
    Исходный рецепт:
    title=${recipe.title}
    ingredients=${recipe.ingredients.joinToString(separator = " | ") { "${it.name}: ${it.amount}" }}
    steps=${recipe.steps.joinToString(separator = " | ")}
    time=${recipe.time}
    calories=${recipe.calories}
    """.trimIndent()

private fun buildFoundRecipesLocalizationPrompt(recipes: List<Recipe>): String =
    """
    Ты локализуешь уже найденные рецепты для русскоязычного приложения.
    Верни только валидный JSON без markdown и пояснений.
    JSON должен быть объектом ровно с одним полем recipes.
    recipes должен быть массивом из ${recipes.size} объектов в том же порядке, что входные recipeIndex.
    Каждый объект recipes должен иметь ровно поля: title, ingredients, steps, time, calories.
    Формат каждого объекта:
    {"title":"...","ingredients":[{"name":"...","amount":"..."}],"steps":["..."],"time":"...","calories":123}
    Переведи существующие рецепты на русский язык без изменения смысла.
    Нельзя добавлять, удалять, объединять, разделять или переставлять ингредиенты и шаги.
    Для каждого recipeIndex количество ingredients и steps должно точно совпадать с исходным.
    В title, ingredients.name, steps и time не должно остаться английских инструкций или непереведённых английских слов.
    Если time отсутствует или явно неизвестен, оцени примерное время приготовления на русском языке; иначе только переведи исходное время.
    Если calories отсутствует, равно 0 или явно неизвестно, оцени примерную калорийность целым числом в ккал.
    Если caloriesKnown=true, не меняй известную калорийность и верни исходное число.
    Поле calories всегда должно быть числом больше 0, если caloriesKnown=false и рецепт съедобный.

    Исходные рецепты:
    ${recipes.mapIndexed { index, recipe -> recipe.toIndexedLocalizationBlock(index) }.joinToString(separator = "\n\n")}
    """.trimIndent()

private fun buildFoundRecipeLocalizationRepairPrompt(recipe: Recipe): String =
    """
    Предыдущая локализация рецепта была отклонена, потому что в ней остались английские слова или инструкции.
    Верни только валидный JSON ровно с полями: title, ingredients, steps, time, calories.
    Переведи ВСЕ пользовательские текстовые поля на русский язык: title, ingredients.name, ingredients.amount, steps, time.
    Не копируй исходные английские предложения. Допускай латиницу только в общеизвестных брендах или географических названиях.
    Сохрани смысл, количество и порядок ингредиентов (${recipe.ingredients.size}) и шагов (${recipe.steps.size}).
    Если calories неизвестны или равны 0, оцени примерную калорийность целым числом в ккал; если caloriesKnown=true, верни исходное число ${recipe.calories}.

    caloriesKnown=${recipe.caloriesKnown}
    Исходный рецепт:
    title=${recipe.title}
    ingredients=${recipe.ingredients.joinToString(separator = " | ") { "${it.name}: ${it.amount}" }}
    steps=${recipe.steps.joinToString(separator = " | ")}
    time=${recipe.time}
    calories=${recipe.calories}
    """.trimIndent()

private fun buildFoundRecipesLocalizationRepairPrompt(recipes: List<Recipe>): String =
    """
    Предыдущая пакетная локализация была отклонена, потому что в части рецептов остались английские слова или инструкции.
    Верни только валидный JSON без markdown и пояснений.
    JSON должен быть объектом ровно с одним полем recipes.
    recipes должен быть массивом из ${recipes.size} объектов в том же порядке, что входные recipeIndex.
    Каждый объект должен иметь ровно поля: title, ingredients, steps, time, calories.
    Переведи ВСЕ пользовательские текстовые поля на русский язык: title, ingredients.name, ingredients.amount, steps, time.
    Не копируй исходные английские предложения. Допускай латиницу только в общеизвестных брендах или географических названиях.
    Для каждого recipeIndex сохрани смысл, количество и порядок ingredients и steps.
    Если calories неизвестны или равны 0, оцени примерную калорийность целым числом в ккал; если caloriesKnown=true, верни исходное число.

    Исходные рецепты:
    ${recipes.mapIndexed { index, recipe -> recipe.toIndexedLocalizationBlock(index) }.joinToString(separator = "\n\n")}
    """.trimIndent()

private fun Recipe.toIndexedLocalizationBlock(index: Int): String =
    """
    recipeIndex=$index
    caloriesKnown=$caloriesKnown
    title=$title
    ingredients=${ingredients.joinToString(separator = " | ") { "${it.name}: ${it.amount}" }}
    steps=${steps.joinToString(separator = " | ")}
    time=$time
    calories=$calories
    """.trimIndent()

private fun RecommendationContext.recipeSearchPromptScope(): String =
    if (searchesAnyProducts) {
        "случайный рецепт; не ограничивайся текущими запасами или выбранными продуктами"
    } else {
        candidateProducts.toPromptStockBlock()
    }

private fun List<Recipe>.mergeLocalizedBatch(localizedRecipes: List<Recipe>): List<Recipe?> =
    mapIndexed { index, original ->
        localizedRecipes
            .getOrNull(index)
            ?.mergeLocalizedFoundRecipe(original = original)
    }

private fun List<Product>.toPromptStockBlock(): String =
    takeIf(List<Product>::isNotEmpty)
        ?.joinToString(separator = "\n") { it.toPromptLine() }
        ?: "нет выбранных продуктов"

private fun AiRecipeGenerationContext.customRecipeProductInstruction(): String =
    if (products.isEmpty()) {
        "Создай случайный рецепт без привязки к текущим запасам; используй любые подходящие продукты, строго учитывая пользовательские предпочтения."
    } else {
        "Создай новый рецепт только по текущим запасам и пользовательским предпочтениям."
    }

private fun UserFoodPreferences.toPromptBlock(products: List<Product>): String =
    """
    preferredCuisines=${preferredCuisines.joinToString().ifBlank { "none" }}
    preferredProductsText=${preferredProducts.joinToString().ifBlank { "none" }}
    preferredProductsFromStock=${productNames(preferredProductIds, products).ifBlank { "none" }}
    dislikedIngredients=${dislikedIngredients.joinToString().ifBlank { "none" }}
    avoidedProductsText=${avoidedProducts.joinToString().ifBlank { "none" }}
    avoidedProductsFromStock=${productNames(avoidedProductIds, products).ifBlank { "none" }}
    allergies=${allergies.joinToString().ifBlank { "none" }}
    dietaryRestrictions=${dietaryRestrictions.joinToString { it.name }.ifBlank { "none" }}
    preferredCategories=${categoryNames(preferredCategoryIds, products).ifBlank { "none" }}
    avoidedCategories=${categoryNames(avoidedCategoryIds, products).ifBlank { "none" }}
    maxCookingTimeMinutes=${maxCookingTimeMinutes ?: "none"}
    preferredDifficulty=${preferredDifficulty?.name ?: "none"}
    servings=${servings ?: "none"}
    """.trimIndent()

private fun productNames(ids: Set<java.util.UUID>, products: List<Product>): String =
    products
        .filter { it.id in ids }
        .joinToString { it.name }

private fun categoryNames(ids: Set<java.util.UUID>, products: List<Product>): String {
    val stockCategories = products.associate { product ->
        product.preferenceCategoryId() to (product.categoryName ?: product.category.displayName())
    }
    val systemCategories = ProductCategory.entries.associate { category ->
        SystemCategoryCatalog.idFor(category) to category.displayName()
    }
    return ids.mapNotNull { id -> stockCategories[id] ?: systemCategories[id] }
        .distinct()
        .joinToString()
}

private fun ProductCategory.displayName(): String =
    SystemCategoryCatalog.categories.firstOrNull { it.id == SystemCategoryCatalog.idFor(this) }?.name ?: name

private fun JsonNode.firstObjectNode(): JsonNode =
    when {
        isObject -> this
        isArray && size() > 0 && first().isObject -> first()
        else -> error("Expected recipe object")
    }

private fun JsonNode.toRecipeList(): List<Recipe> =
    when {
        isObject -> listOfNotNull(toRecipeOrNull())
            .takeIf(List<Recipe>::isNotEmpty)
            ?: listOf("recipes", "recipe", "data", "result", "items")
                .firstNotNullOfOrNull { fieldName ->
                    path(fieldName)
                        .takeUnless { it.isMissingNode || it.isNull }
                        ?.toRecipeList()
                        ?.takeIf(List<Recipe>::isNotEmpty)
                }
            ?: properties()
                .asSequence()
                .flatMap { (_, node) -> node.toRecipeList().asSequence() }
                .toList()
                .takeIf(List<Recipe>::isNotEmpty)
            ?: emptyList()
        isArray -> flatMap { node -> node.toRecipeList() }
        else -> emptyList()
    }

private fun String.extractJsonPayload(): String {
    val trimmed = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = trimmed.indexOfFirst { it == '{' || it == '[' }
    val end = trimmed.indexOfLast { it == '}' || it == ']' }
    require(start >= 0 && end >= start) { "Expected JSON payload" }
    return trimmed.substring(start, end + 1)
}

private data class GigaChatRecipeRequest(
    val model: String,
    val messages: List<GigaChatMessage>,
    val temperature: Double
) {
    companion object {
        fun from(prompt: String): GigaChatRecipeRequest =
            GigaChatRecipeRequest(
                model = "GigaChat",
                messages = listOf(
                    GigaChatMessage(
                        role = "system",
                        content = "Ты генератор рецептов для русскоязычного приложения. Отвечай строго валидным JSON без пояснений, весь пользовательский текст в JSON пиши по-русски."
                    ),
                    GigaChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.2
            )
    }
}

private data class GigaChatMessage(
    val role: String,
    val content: String
)

private fun JsonNode.toRecipeOrNull(): Recipe? {
    val safeTitle = firstFieldText("title", "name", "recipeTitle", "recipe_title") ?: return null
    val safeIngredients = path("ingredients")
        .takeIf(JsonNode::isArray)
        ?.mapNotNull(JsonNode::toRecipeIngredientOrNull)
        .orEmpty()
    val safeSteps = firstArray("steps", "instructions", "directions", "method")
        ?.mapNotNull(JsonNode::valueText)
        ?: firstFieldText("steps", "instructions", "directions", "method")
            ?.splitRecipeSteps()
        ?: emptyList()
    val safeTime = firstFieldText("time", "cookingTime", "cooking_time", "cookTime", "cook_time", "duration")
        ?: safeSteps.estimateTime()
    val safeCalories = caloriesNode().toLenientCaloriesOrNull() ?: return null
    if (safeIngredients.isEmpty() || safeSteps.isEmpty()) return null
    val calories = safeCalories.value.takeIf { it > 0 } ?: safeIngredients.estimateCalories()

    return Recipe(
        title = safeTitle,
        ingredients = safeIngredients,
        steps = safeSteps,
        time = safeTime,
        calories = calories,
        caloriesKnown = true
    )
}

private fun JsonNode.fieldText(name: String): String? =
    path(name).valueText()

private fun JsonNode.firstFieldText(vararg names: String): String? =
    names.firstNotNullOfOrNull { name -> fieldText(name) }

private fun JsonNode.firstArray(vararg names: String): JsonNode? =
    names.asSequence()
        .map { name -> path(name) }
        .firstOrNull { node -> node.isArray }

private fun JsonNode.toRecipeIngredientOrNull(): RecipeIngredient? {
    if (isTextual) {
        val text = valueText() ?: return null
        val parts = text.split(":", " - ", " — ", limit = 2)
        return RecipeIngredient(
            name = parts.first().trim(),
            amount = parts.getOrNull(1)?.trim()?.takeIf(String::isNotBlank) ?: "по вкусу"
        )
    }
    val name = firstFieldText("name", "ingredient", "title")
    val amount = firstFieldText("amount", "measure", "quantity", "qty")
    return if (name == null || amount == null) null else RecipeIngredient(name, amount)
}

private fun String.splitRecipeSteps(): List<String> =
    split(Regex("""(?:\r?\n)+|(?:^|\s)\d+[\).]\s+"""))
        .map(String::trim)
        .filter(String::isNotBlank)
        .takeIf(List<String>::isNotEmpty)
        ?: listOf(trim()).filter(String::isNotBlank)

private fun List<String>.estimateTime(): String =
    "${(size.coerceAtLeast(1) * 10).coerceIn(15, 90)} минут"

private fun JsonNode.caloriesNode(): JsonNode =
    calorieFieldNames
        .asSequence()
        .map { fieldName -> path(fieldName) }
        .firstOrNull { node -> !node.isMissingNode && !node.isNull }
        ?: path("nutrition").let { nutrition ->
            calorieFieldNames
                .asSequence()
                .map { fieldName -> nutrition.path(fieldName) }
                .firstOrNull { node -> !node.isMissingNode && !node.isNull }
        }
        ?: path("nutritionFacts").let { nutrition ->
            calorieFieldNames
                .asSequence()
                .map { fieldName -> nutrition.path(fieldName) }
                .firstOrNull { node -> !node.isMissingNode && !node.isNull }
        }
        ?: path("calories")

private fun JsonNode.valueText(): String? =
    when {
        isMissingNode || isNull -> null
        isTextual -> asText()
        isNumber || isBoolean -> asText()
        else -> null
    }?.trim()?.takeIf(String::isNotEmpty)

private fun JsonNode.toNonNegativeIntOrNull(): Int? =
    when {
        isNumber -> asDouble().takeIf { it >= 0.0 }?.toInt()
        isTextual -> numberPattern.find(asText())?.value?.toDoubleOrNull()?.takeIf { it >= 0.0 }?.toInt()
        else -> null
    }

private fun JsonNode.toLenientCaloriesOrNull(): ParsedCalories? =
    when {
        isMissingNode || isNull -> null
        else -> toNonNegativeIntOrNull()
            ?.let { ParsedCalories(value = it, known = true) }
            ?: ParsedCalories(value = 0, known = false)
    }

private fun Recipe.mergeLocalizedFoundRecipe(original: Recipe): Recipe? =
    mergeLocalizedFoundRecipeData(original)
        .takeIf(Recipe::isAcceptablyLocalized)

private fun Recipe.mergeLocalizedFoundRecipeData(original: Recipe): Recipe {
    val mergedIngredients = when {
        ingredients.size == original.ingredients.size -> ingredients.mapIndexed { index, ingredient ->
            RecipeIngredient(
                name = ingredient.name.takeIf(String::isNotBlank) ?: original.ingredients[index].name,
                amount = ingredient.amount.takeIf(String::isNotBlank) ?: original.ingredients[index].amount
            )
        }
        ingredients.isNotEmpty() -> ingredients.map { ingredient ->
            RecipeIngredient(
                name = ingredient.name,
                amount = ingredient.amount.takeIf(String::isNotBlank) ?: "по вкусу"
            )
        }
        else -> original.ingredients
    }
    val mergedSteps = when {
        steps.size == original.steps.size -> steps.mapIndexed { index, step ->
            step.takeIf(String::isNotBlank) ?: original.steps[index]
        }
        steps.isNotEmpty() -> steps
        else -> original.steps
    }
    val mergedCalories = if (original.caloriesKnown && original.calories > 0) {
        original.calories
    } else {
        calories.takeIf { it > 0 } ?: mergedIngredients.estimateCalories()
    }
    return Recipe(
        title = title.takeIf(String::isNotBlank) ?: original.title,
        ingredients = mergedIngredients,
        steps = mergedSteps,
        time = time.takeIf(String::isNotBlank) ?: original.time,
        calories = mergedCalories,
        caloriesKnown = true
    )
}

private fun List<RecipeIngredient>.estimateCalories(): Int =
    (size.coerceAtLeast(1) * 120).coerceIn(120, 900)

private fun Recipe.isAcceptablyLocalized(): Boolean {
    val localizedTextFields = listOf(title, time) + steps
    return localizedTextFields.none(String::containsUnlocalizedLatinText)
}

private fun String.containsUnlocalizedLatinText(): Boolean {
    val latinTokens = latinWordPattern.findAll(this).map { it.value }.toList()
    if (latinTokens.isEmpty()) return false

    val cyrillicCount = count { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
    val latinCount = latinTokens.sumOf(String::length)
    return cyrillicCount == 0 ||
        latinCount > cyrillicCount ||
        latinTokens.any { token -> token.lowercase() in commonEnglishRecipeWords }
}

private data class ParsedCalories(
    val value: Int,
    val known: Boolean
)

private val numberPattern = Regex("""\d+(?:\.\d+)?""")

private val latinWordPattern = Regex("""\b[A-Za-z]{3,}\b""")

private val commonEnglishRecipeWords = setOf(
    "add",
    "bake",
    "boil",
    "bring",
    "chop",
    "clear",
    "cook",
    "cover",
    "directions",
    "heat",
    "ingredients",
    "low",
    "mayonnaise",
    "method",
    "minutes",
    "mix",
    "pan",
    "place",
    "pot",
    "ready",
    "reduce",
    "rice",
    "rinse",
    "serve",
    "servings",
    "slice",
    "stir",
    "tender",
    "until",
    "water",
    "with"
)

private val calorieFieldNames = listOf(
    "calories",
    "caloriesKcal",
    "calories_kcal",
    "caloriesPerServing",
    "calories_per_serving",
    "estimatedCalories",
    "estimated_calories",
    "approxCalories",
    "approx_calories",
    "kcal",
    "kilocalories",
    "energyKcal",
    "energy_kcal"
)
