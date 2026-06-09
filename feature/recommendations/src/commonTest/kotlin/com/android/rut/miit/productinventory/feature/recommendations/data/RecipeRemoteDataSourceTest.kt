package com.android.rut.miit.productinventory.feature.recommendations.data

import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class RecipeRemoteDataSourceTest {

    @Test
    fun `get recipes decodes strict recipe payload`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes", request.url.encodedPath)
            respondStrictRecipeJson()
        }

        val recipes = RecipeRemoteDataSource(httpClient(engine)).getRecipes("household-id")

        assertEquals("Rice Bowl", recipes.single().title)
        assertEquals("rice", recipes.single().ingredients.single().name)
        assertEquals("1 cup", recipes.single().ingredients.single().amount)
        assertEquals(listOf("Cook rice"), recipes.single().steps)
        assertEquals("15 minutes", recipes.single().time)
        assertEquals(300, recipes.single().calories)
    }

    @Test
    fun `get recipe suggestions keeps legacy suggestions endpoint`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes/suggestions", request.url.encodedPath)
            respondStrictRecipeJson()
        }

        val recipes = RecipeRemoteDataSource(httpClient(engine)).getRecipeSuggestions("household-id")

        assertEquals("Rice Bowl", recipes.single().title)
    }

    @Test
    fun `get recipes sends current products mode by default`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes", request.url.encodedPath)
            assertEquals(RecommendationMode.CURRENT_PRODUCTS.name, request.url.parameters["mode"])
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val recipes = RecipeRemoteDataSource(httpClient(engine)).getRecipes("household-id")

        assertTrue(recipes.isEmpty())
    }

    @Test
    fun `get recipes sends use soon mode when requested`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes", request.url.encodedPath)
            assertEquals(RecommendationMode.USE_SOON.name, request.url.parameters["mode"])
            respondStrictRecipeJson()
        }

        val recipes = RecipeRemoteDataSource(httpClient(engine))
            .getRecipes("household-id", RecommendationMode.USE_SOON)

        assertEquals("Rice Bowl", recipes.single().title)
    }

    @Test
    fun `get ingredient options decodes current products`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes/ingredients", request.url.encodedPath)
            respond(
                content = """
                    [
                      {
                        "id": "product-id",
                        "name": "Рис",
                        "categoryName": "Крупы",
                        "remainingAmount": 1.0,
                        "unit": "PIECES",
                        "expiring": false
                      }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val options = RecipeRemoteDataSource(httpClient(engine)).getIngredientOptions("household-id")

        assertEquals("Рис", options.single().name)
    }

    @Test
    fun `get ingredient options decodes product availability metadata`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes/ingredients", request.url.encodedPath)
            respond(
                content = """
                    [
                      {
                        "id": "product-id",
                        "name": "Йогурт",
                        "categoryName": "Молочные продукты",
                        "remainingAmount": 0.5,
                        "unit": "LITERS",
                        "expiring": true
                      }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val option = RecipeRemoteDataSource(httpClient(engine)).getIngredientOptions("household-id").single()

        assertEquals("product-id", option.id)
        assertEquals("Молочные продукты", option.categoryName)
        assertEquals(0.5, option.remainingAmount)
        assertEquals("LITERS", option.unit)
        assertTrue(option.expiring)
    }

    @Test
    fun `find recipes posts selected product ids`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes/search", request.url.encodedPath)
            respondStrictRecipeJson()
        }

        val recipes = RecipeRemoteDataSource(httpClient(engine))
            .findRecipes("household-id", com.android.rut.miit.productinventory.feature.recommendations.data.models.FindRecipeRequestDto(setOf("product-id")))

        assertEquals("Rice Bowl", recipes.single().title)
    }

    @Test
    fun `generate ai recipe decodes ai metadata from object response`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/recipes/ai-generated", request.url.encodedPath)
            respond(
                content = """
                    {
                      "id": "ai-recipe-id",
                      "title": "AI Supper",
                      "ingredients": [{"name": "potato", "amount": "2 pcs"}],
                      "steps": ["Bake potatoes"],
                      "time": "25 minutes",
                      "cookingTimeMinutes": 25,
                      "calories": 0,
                      "caloriesKnown": false,
                      "source": "GIGACHAT",
                      "warnings": ["Проверьте ингредиенты"],
                      "aiAssisted": true,
                      "aiGenerated": true
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val recipe = RecipeRemoteDataSource(httpClient(engine)).generateAiRecipe(
            "household-id",
            com.android.rut.miit.productinventory.feature.recommendations.data.models.GenerateAiRecipeRequestDto(
                maxCookingTimeMinutes = 30,
                servings = 2
            )
        )

        assertEquals("ai-recipe-id", recipe.id)
        assertEquals(25, recipe.cookingTimeMinutes)
        assertEquals("GIGACHAT", recipe.source)
        assertEquals(listOf("Проверьте ингредиенты"), recipe.warnings)
        assertTrue(recipe.aiGenerated)
        assertTrue(recipe.aiAssisted)
        assertTrue(!recipe.caloriesKnown)
    }

    @Test
    fun `recipe discovery requests override timeout without affecting suggestions`() = runTest {
        val observedTimeouts = mutableMapOf<String, Long?>()
        val engine = MockEngine { request ->
            observedTimeouts[request.url.encodedPath] =
                request.getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis
            if (request.url.encodedPath.endsWith("/ai-generated")) {
                respondStrictRecipeObjectJson()
            } else {
                respondStrictRecipeJson()
            }
        }
        val dataSource = RecipeRemoteDataSource(httpClient(engine))

        dataSource.getRecipes("household-id")
        dataSource.findRecipes(
            "household-id",
            com.android.rut.miit.productinventory.feature.recommendations.data.models.FindRecipeRequestDto(emptySet())
        )
        dataSource.generateAiRecipe(
            "household-id",
            com.android.rut.miit.productinventory.feature.recommendations.data.models.GenerateAiRecipeRequestDto()
        )
        dataSource.getRecipeSuggestions("household-id")

        assertEquals(60_000L, observedTimeouts["/api/v1/households/household-id/recipes"])
        assertEquals(60_000L, observedTimeouts["/api/v1/households/household-id/recipes/search"])
        assertEquals(60_000L, observedTimeouts["/api/v1/households/household-id/recipes/ai-generated"])
        assertEquals(12_000L, observedTimeouts["/api/v1/households/household-id/recipes/suggestions"])
    }

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(HttpTimeout) {
                requestTimeoutMillis = 12_000
                socketTimeoutMillis = 12_000
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }

    private fun MockRequestHandleScope.respondStrictRecipeJson() =
        respond(
            content = """
                [
                  {
                    "title": "Rice Bowl",
                    "ingredients": [
                      {
                        "name": "rice",
                        "amount": "1 cup"
                      }
                    ],
                    "steps": ["Cook rice"],
                    "time": "15 minutes",
                    "calories": 300
                  }
                ]
            """.trimIndent(),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )

    private fun MockRequestHandleScope.respondStrictRecipeObjectJson() =
        respond(
            content = """
                {
                  "title": "Rice Bowl",
                  "ingredients": [
                    {
                      "name": "rice",
                      "amount": "1 cup"
                    }
                  ],
                  "steps": ["Cook rice"],
                  "time": "15 minutes",
                  "calories": 300
                }
            """.trimIndent(),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )
}
