package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.recipe

import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSearchScope
import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class TheMealDbRecipeSearchProviderTest {

    @Test
    fun `searches by mapped ingredient and maps meal lookup response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = TheMealDbRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://themealdb.test/api/json/v1/1",
            maxProducts = 1,
            maxRecipes = 1
        )

        server.expect(requestTo("https://themealdb.test/api/json/v1/1/filter.php?i=rice"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"meals":[{"idMeal":"52772","strMeal":"Teriyaki Chicken Casserole"}]}""",
                    MediaType.APPLICATION_JSON
                )
            )
        server.expect(requestTo("https://themealdb.test/api/json/v1/1/lookup.php?i=52772"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "meals": [
                        {
                          "idMeal": "52772",
                          "strMeal": "Teriyaki Chicken Casserole",
                          "strInstructions": "Cook rice.\nBake casserole.",
                          "strMealThumb": "https://themealdb.test/images/52772.jpg",
                          "strIngredient1": "rice",
                          "strMeasure1": "1 cup",
                          "strIngredient2": "chicken",
                          "strMeasure2": "300 g"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val results = provider.searchRecipes(context(product("Рис")))

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals(RecipeSource.EXTERNAL_API, result.source)
        assertEquals("Teriyaki Chicken Casserole", result.recipe.title)
        assertEquals("https://themealdb.test/api/json/v1/1/lookup.php?i=52772", result.sourceUrl)
        assertEquals("https://themealdb.test/images/52772.jpg", result.imageUrl)
        assertEquals(listOf("Cook rice.", "Bake casserole."), result.recipe.steps)
        assertEquals(listOf("rice", "chicken"), result.recipe.ingredients.map { it.name })
        assertEquals(listOf("1 cup", "300 g"), result.recipe.ingredients.map { it.amount })
        assertEquals(false, result.recipe.caloriesKnown)
        server.verify()
    }

    @Test
    fun `searches random meal without current products`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = TheMealDbRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://themealdb.test/api/json/v1/1",
            maxProducts = 1,
            maxRecipes = 1
        )

        server.expect(requestTo("https://themealdb.test/api/json/v1/1/random.php"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "meals": [
                        {
                          "idMeal": "52900",
                          "strMeal": "Random Curry",
                          "strInstructions": "Mix spices.\nSimmer sauce.",
                          "strMealThumb": "https://themealdb.test/images/52900.jpg",
                          "strIngredient1": "chickpeas",
                          "strMeasure1": "400 g"
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val results = provider.searchRandomRecipes(randomContext())

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals(RecipeSource.EXTERNAL_API, result.source)
        assertEquals("Random Curry", result.recipe.title)
        assertEquals("https://themealdb.test/api/json/v1/1/lookup.php?i=52900", result.sourceUrl)
        assertEquals(listOf("chickpeas"), result.recipe.ingredients.map { it.name })
        assertEquals(listOf("400 g"), result.recipe.ingredients.map { it.amount })
        server.verify()
    }

    @Test
    fun `random search stays random and returns empty when random endpoint has no meals`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = TheMealDbRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://themealdb.test/api/json/v1/1",
            maxProducts = 1,
            maxRecipes = 1
        )

        server.expect(requestTo("https://themealdb.test/api/json/v1/1/random.php"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"meals":null}""", MediaType.APPLICATION_JSON))

        val results = provider.searchRandomRecipes(randomContext(product("Рис")))

        assertEquals(emptyList(), results)
        server.verify()
    }

    private fun context(product: Product): RecommendationContext =
        RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = product.householdId,
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = listOf(product),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID())
        )

    private fun randomContext(product: Product? = null): RecommendationContext =
        RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = listOfNotNull(product),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID()),
            searchScope = RecipeSearchScope.ANY_PRODUCTS
        )

    private fun product(name: String): Product =
        Product(
            name = name,
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            expirationDate = ExpirationDate(LocalDate.now().plusDays(5)),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
}
