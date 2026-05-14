package com.android.rut.miit.productinventory.feature.recommendations.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
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

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
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
}
