package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.AiRateLimiter
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai.GigaChatAccessTokenProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.hamcrest.Matchers.containsString

class GigaChatRecipeProviderTest {

    @Test
    fun `returns strict recipe from GigaChat response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andExpect(content().string(containsString("русскоязычного приложения")))
            .andExpect(content().string(containsString("должны быть на русском языке")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Рисовая тарелка\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Отварите рис\"],\"time\":\"15 минут\",\"calories\":300}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals("Рисовая тарелка", recipes.single().title)
        assertEquals(listOf(RecipeIngredient("рис", "1 стакан")), recipes.single().ingredients)
        assertEquals(listOf("Отварите рис"), recipes.single().steps)
        assertEquals("15 минут", recipes.single().time)
        assertEquals(300, recipes.single().calories)
        server.verify()
    }

    @Test
    fun `does not fall back to local recipe documents when GigaChat is unavailable`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(withServerError())

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals(emptyList(), recipes)
        server.verify()
    }

    @Test
    fun `parses recipe JSON wrapped in markdown fence`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "```json\n{\"title\":\"Rice Bowl\",\"ingredients\":[{\"name\":\"rice\",\"amount\":\"1 cup\"}],\"steps\":[\"Cook rice\"],\"time\":\"15 minutes\",\"calories\":300}\n```"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals("Rice Bowl", recipes.single().title)
        server.verify()
    }

    @Test
    fun `parses recipe list wrapped in recipes field`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"recipes\":[{\"title\":\"Рис с овощами\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Сварите рис\"],\"time\":\"20 минут\",\"calories\":250},{\"title\":\"Рисовый суп\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"0.5 стакана\"}],\"steps\":[\"Сварите суп\"],\"time\":\"30 минут\",\"calories\":180}]}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.searchWebRecipes(randomContext())

        assertEquals(listOf("Рис с овощами", "Рисовый суп"), recipes.map { it.recipe.title })
        server.verify()
    }

    @Test
    fun `accepts unknown textual calories from GigaChat as explicit unknown calories`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "```json\n{\"title\":\"Рис с овощами\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Отварите рис\"],\"time\":\"15 минут\",\"calories\":\"неизвестно\"}\n```"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals("Рис с овощами", recipes.single().title)
        assertEquals(120, recipes.single().calories)
        assertEquals(true, recipes.single().caloriesKnown)
        server.verify()
    }

    @Test
    fun `parses calories from nutrition aliases`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Рис с овощами\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Отварите рис\"],\"time\":\"15 минут\",\"nutrition\":{\"calories\":\"420 ккал\"}}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals(420, recipes.single().calories)
        assertEquals(true, recipes.single().caloriesKnown)
        server.verify()
    }

    @Test
    fun `batch localizes found recipes with one GigaChat request`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andExpect(content().string(containsString("recipes должен быть массивом из 2 объектов")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"recipes\":[{\"title\":\"Курица с рисом\",\"ingredients\":[{\"name\":\"курица\",\"amount\":\"300 г\"}],\"steps\":[\"Приготовьте курицу\"],\"time\":\"25 минут\",\"calories\":320},{\"title\":\"Рисовый суп\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Сварите суп\"],\"time\":\"30 минут\",\"calories_kcal\":210}]}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipes = provider.localizeAndEnrichFoundRecipes(
            listOf(
                Recipe(
                    title = "Chicken rice",
                    ingredients = listOf(RecipeIngredient("chicken", "300 g")),
                    steps = listOf("Cook chicken"),
                    time = "25 minutes",
                    calories = 0,
                    caloriesKnown = false
                ),
                Recipe(
                    title = "Rice soup",
                    ingredients = listOf(RecipeIngredient("rice", "1 cup")),
                    steps = listOf("Cook soup"),
                    time = "30 minutes",
                    calories = 0,
                    caloriesKnown = false
                )
            )
        )

        assertEquals(listOf("Курица с рисом", "Рисовый суп"), recipes.map { it?.title })
        assertEquals(listOf(320, 210), recipes.map { it?.calories })
        server.verify()
    }

    @Test
    fun `localizes found recipe without overwriting known calories or changing structure`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andExpect(content().string(containsString("Переведи существующий рецепт на русский язык")))
            .andExpect(content().string(containsString("Нельзя добавлять, удалять, объединять, разделять или переставлять ингредиенты и шаги")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Куриный суп\",\"ingredients\":[{\"name\":\"курица\",\"amount\":\"300 г\"},{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Отварите курицу\",\"Добавьте рис\"],\"time\":\"25 минут\",\"calories\":999}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipe = provider.localizeAndEnrichFoundRecipe(
            Recipe(
                title = "Chicken Soup",
                ingredients = listOf(RecipeIngredient("chicken", "300 g"), RecipeIngredient("rice", "1 cup")),
                steps = listOf("Boil chicken", "Add rice"),
                time = "25 minutes",
                calories = 240,
                caloriesKnown = true
            )
        )

        assertEquals("Куриный суп", recipe?.title)
        assertEquals(listOf(RecipeIngredient("курица", "300 г"), RecipeIngredient("рис", "1 стакан")), recipe?.ingredients)
        assertEquals(listOf("Отварите курицу", "Добавьте рис"), recipe?.steps)
        assertEquals("25 минут", recipe?.time)
        assertEquals(240, recipe?.calories)
        assertEquals(true, recipe?.caloriesKnown)
        server.verify()
    }

    @Test
    fun `accepts localized found recipe with Latin brand in ingredient name`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Курица с острым соусом\",\"ingredients\":[{\"name\":\"соус Sriracha\",\"amount\":\"1 ст. л.\"},{\"name\":\"курица\",\"amount\":\"300 г\"}],\"steps\":[\"Смешайте соус с курицей\",\"Обжарьте до готовности\"],\"time\":\"25 минут\",\"calories\":320}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipe = provider.localizeAndEnrichFoundRecipe(
            Recipe(
                title = "Chicken with Sriracha",
                ingredients = listOf(RecipeIngredient("Sriracha sauce", "1 tbsp"), RecipeIngredient("chicken", "300 g")),
                steps = listOf("Mix sauce with chicken", "Cook until done"),
                time = "25 minutes",
                calories = 0,
                caloriesKnown = false
            )
        )

        assertEquals(listOf("соус Sriracha", "курица"), recipe?.ingredients?.map(RecipeIngredient::name))
        assertEquals(2, recipe?.ingredients?.size)
        assertEquals(2, recipe?.steps?.size)
        server.verify()
    }

    @Test
    fun `repairs found recipe localization that leaves raw English steps`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Курица с рисом\",\"ingredients\":[{\"name\":\"курица\",\"amount\":\"300 г\"}],\"steps\":[\"Place rice in a fine-mesh sieve and rinse until water runs clear\"],\"time\":\"25 минут\",\"calories\":320}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andExpect(content().string(containsString("была отклонена")))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Курица с рисом\",\"ingredients\":[{\"name\":\"курица\",\"amount\":\"300 г\"}],\"steps\":[\"Промойте рис в мелком сите, пока вода не станет прозрачной\"],\"time\":\"25 минут\",\"calories\":320}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipe = provider.localizeAndEnrichFoundRecipe(
            Recipe(
                title = "Chicken rice",
                ingredients = listOf(RecipeIngredient("chicken", "300 g")),
                steps = listOf("Cook chicken"),
                time = "25 minutes",
                calories = 0,
                caloriesKnown = false
            )
        )

        assertEquals("Курица с рисом", recipe?.title)
        assertEquals(listOf("Промойте рис в мелком сите, пока вода не станет прозрачной"), recipe?.steps)
        server.verify()
    }

    @Test
    fun `localizer fills unknown calories from approximate ai estimate`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        expectOAuth(server)
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Рис с курицей\",\"ingredients\":[{\"name\":\"рис\",\"amount\":\"1 стакан\"}],\"steps\":[\"Приготовьте рис\"],\"time\":\"20 минут\",\"calories\":310}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val recipe = provider.localizeAndEnrichFoundRecipe(
            Recipe(
                title = "Rice Chicken",
                ingredients = listOf(RecipeIngredient("rice", "1 cup")),
                steps = listOf("Cook rice"),
                time = "Время зависит от рецепта",
                calories = 0,
                caloriesKnown = false
            )
        )

        assertEquals("20 минут", recipe?.time)
        assertEquals(310, recipe?.calories)
        assertEquals(true, recipe?.caloriesKnown)
        server.verify()
    }

    @Test
    fun `does not call GigaChat without api key and returns no local fallback`() {
        val provider = provider(apiKey = "")

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals(emptyList(), recipes)
    }

    private fun provider(
        builder: RestClient.Builder = RestClient.builder(),
        apiKey: String
    ): GigaChatRecipeProvider =
        GigaChatRecipeProvider(
            restClientBuilder = builder,
            objectMapper = ObjectMapper().registerKotlinModule(),
            rateLimiter = AiRateLimiter(30),
            accessTokenProvider = GigaChatAccessTokenProvider(
                restClientBuilder = builder,
                apiKey = apiKey,
                oauthUrl = "https://oauth.test/token",
                scope = "GIGACHAT_API_PERS"
            ),
            baseUrl = "https://gigachat.test",
            retryAttempts = 1,
            retryBackoffMs = 0
        )

    private fun expectOAuth(server: MockRestServiceServer) {
        server.expect(requestTo("https://oauth.test/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Basic secret"))
            .andRespond(
                withSuccess(
                    """{"access_token":"access-token","expires_at":4102444800000}""",
                    MediaType.APPLICATION_JSON
                )
            )
    }

    private fun product(name: String, category: ProductCategory): Product =
        Product(
            name = name,
            category = category,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

    private fun randomContext(): com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext =
        com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = com.android.rut.miit.productinventory.domain.model.RecommendationMode.CURRENT_PRODUCTS,
            products = emptyList(),
            expiringProducts = emptyList(),
            preferences = com.android.rut.miit.productinventory.domain.model.UserFoodPreferences.empty(UUID.randomUUID()),
            searchScope = com.android.rut.miit.productinventory.application.service.recommendation.RecipeSearchScope.ANY_PRODUCTS
        )
}
