package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class GigaChatRecipeProviderTest {

    @Test
    fun `returns strict recipe from GigaChat response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer secret"))
            .andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"Rice Bowl\",\"ingredients\":[{\"name\":\"rice\",\"amount\":\"1 cup\"}],\"steps\":[\"Cook rice\"],\"time\":\"15 minutes\",\"calories\":300}"
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
        assertEquals(listOf(RecipeIngredient("rice", "1 cup")), recipes.single().ingredients)
        assertEquals(listOf("Cook rice"), recipes.single().steps)
        assertEquals("15 minutes", recipes.single().time)
        assertEquals(300, recipes.single().calories)
        server.verify()
    }

    @Test
    fun `falls back to retrieved recipe when GigaChat is unavailable`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = provider(builder = builder, apiKey = "secret")
        server.expect(requestTo("https://gigachat.test/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withServerError())

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals("Fallback Rice", recipes.single().title)
        assertEquals(listOf(RecipeIngredient("rice", "1 cup")), recipes.single().ingredients)
        server.verify()
    }

    @Test
    fun `does not call GigaChat without api key and returns RAG fallback`() {
        val provider = provider(apiKey = "")

        val recipes = provider.findRecipes(RecipeGenerationRequest(listOf(product("Rice", ProductCategory.CEREALS))))

        assertEquals("Fallback Rice", recipes.single().title)
    }

    private fun provider(
        builder: RestClient.Builder = RestClient.builder(),
        apiKey: String
    ): GigaChatRecipeProvider =
        GigaChatRecipeProvider(
            restClientBuilder = builder,
            recipeRetriever = RecipeRetriever(FakeRecipeKnowledgeRepository(listOf(document()))),
            objectMapper = ObjectMapper().registerKotlinModule(),
            baseUrl = "https://gigachat.test",
            apiKey = apiKey
        )

    private fun document(): RecipeDocument =
        RecipeDocument(
            id = "fallback-rice",
            title = "Fallback Rice",
            ingredients = listOf(RecipeIngredient("rice", "1 cup")),
            steps = listOf("Cook rice"),
            time = "15 minutes",
            calories = 300,
            requiredIngredients = setOf("rice"),
            categories = setOf(ProductCategory.CEREALS),
            rules = listOf("Use available rice")
        )

    private fun product(name: String, category: ProductCategory): Product =
        Product(
            name = name,
            category = category,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

    private class FakeRecipeKnowledgeRepository(
        private val documents: List<RecipeDocument>
    ) : IRecipeKnowledgeRepository {
        override fun findAll(): List<RecipeDocument> = documents
    }
}
