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
import kotlin.test.assertFalse
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets

class PovarRuRecipeSearchProviderTest {

    @Test
    fun `searches Russian category page and maps detailed recipe`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = PovarRuRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://povar.test",
            maxProducts = 1,
            maxRecipes = 1
        )

        server.expect(requestTo("https://povar.test/list/kurica/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeListHtml(), utf8Html))
        server.expect(requestTo("https://povar.test/recipes/salat_podsolnuh-821.html"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeDetailsHtml(), utf8Html))

        val results = provider.searchRecipes(context(product("Курица")))

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals(RecipeSource.EXTERNAL_API, result.source)
        assertEquals(false, result.requiresLocalization)
        assertEquals("Салат Подсолнух", result.recipe.title)
        assertEquals("https://povar.test/recipes/salat_podsolnuh-821.html", result.sourceUrl)
        assertEquals("https://img.povar.test/main.jpg", result.imageUrl)
        assertEquals("30 мин", result.recipe.time)
        assertEquals(247, result.recipe.calories)
        assertEquals(listOf("Куриная грудка", "Шампиньоны"), result.recipe.ingredients.map { it.name })
        assertEquals(listOf("300 грамм (отварная)", "300 грамм"), result.recipe.ingredients.map { it.amount })
        assertEquals(
            listOf(
                "Яйца отварите. Куриную грудку нарежьте.",
                "Выложите салат слоями."
            ),
            result.recipe.steps
        )
        server.verify()
    }

    @Test
    fun `uses generic Povar search for products without category alias`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = PovarRuRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://povar.test",
            maxProducts = 1,
            maxRecipes = 1
        )
        val searchUri = UriComponentsBuilder.fromUriString("https://povar.test")
            .path("/xmlsearch")
            .queryParam("query", "гречка")
            .build()
            .encode()
            .toUri()

        server.expect(requestTo(searchUri))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeListHtml(title = "Гречка без варки"), utf8Html))
        server.expect(requestTo("https://povar.test/recipes/salat_podsolnuh-821.html"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeDetailsHtml(title = "Гречка без варки"), utf8Html))

        val results = provider.searchRecipes(context(product("Гречка")))

        assertEquals(1, results.size)
        assertEquals("Гречка без варки", results.single().recipe.title)
        assertFalse(results.single().requiresLocalization)
        server.verify()
    }

    @Test
    fun `random search rotates Russian category pages without localization`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = PovarRuRecipeSearchProvider(
            restClientBuilder = builder,
            enabled = true,
            baseUrl = "https://povar.test",
            maxProducts = 1,
            maxRecipes = 1
        )

        server.expect(requestTo("https://povar.test/list/salad/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeListHtml(title = "Случайный салат"), utf8Html))
        server.expect(requestTo("https://povar.test/recipes/salat_podsolnuh-821.html"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeDetailsHtml(title = "Случайный салат"), utf8Html))
        server.expect(requestTo("https://povar.test/list/soup/"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeListHtml(title = "Случайный суп"), utf8Html))
        server.expect(requestTo("https://povar.test/recipes/salat_podsolnuh-821.html"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(recipeDetailsHtml(title = "Случайный суп"), utf8Html))

        val results = provider.searchRandomRecipes(randomContext(product("Гречка")))
        val rotatedResults = provider.searchRandomRecipes(randomContext(product("Гречка")))

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals(RecipeSource.EXTERNAL_API, result.source)
        assertEquals("Случайный салат", result.recipe.title)
        assertEquals("Случайный рецепт найден в русскоязычном внешнем источнике Povar.ru", result.reasons.single())
        assertFalse(result.requiresLocalization)
        assertEquals("Случайный суп", rotatedResults.single().recipe.title)
        assertFalse(rotatedResults.single().requiresLocalization)
        server.verify()
    }

    private fun recipeListHtml(title: String = "Салат Подсолнух"): String =
        """
        <html><body>
          <div class="recipe_list">
            <div class="recipe">
              <div class="h3">
                <a href="/recipes/salat_podsolnuh-821.html" class="listRecipieTitle">$title</a>
              </div>
              <span class="a thumb">
                <img src="https://img.povar.test/list.jpg" alt="$title"/>
              </span>
            </div>
          </div>
        </body></html>
        """.trimIndent()

    private fun recipeDetailsHtml(title: String = "Салат Подсолнух"): String =
        """
        <html><body>
          <div itemscope itemtype="https://schema.org/Recipe">
            <h1 class="detailed fn" itemprop="name">$title</h1>
            <div class="bigImgBox">
              <img itemprop="image" src="https://img.povar.test/main.jpg"/>
            </div>
            <span class="duration">30 мин</span>
            <ul class="detailed_ingredients">
              <li itemprop="recipeIngredient" class="ingredient flex-dot-line" rel="Куриная грудка">
                <span class="name">Куриная грудка</span>
                <span class="value">300</span>
                <span class="u-unit-name">грамм</span>
                <span class="descr">(отварная)</span>
              </li>
              <li itemprop="recipeIngredient" class="ingredient flex-dot-line" rel="Шампиньоны">
                <span class="name">Шампиньоны</span>
                <span class="value">300</span>
                <span class="u-unit-name">грамм</span>
              </li>
            </ul>
            <span itemprop="calories">247 ккал</span>
            <div itemprop="recipeInstructions" class="instructions">
              <div class="detailed_step_description_big">1. Яйца отварите. Куриную грудку нарежьте.</div>
              <div class="detailed_step_description_big">2. Выложите салат слоями.</div>
            </div>
          </div>
        </body></html>
        """.trimIndent()

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
            householdId = product?.householdId ?: UUID.randomUUID(),
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = listOfNotNull(product),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID()),
            searchScope = RecipeSearchScope.ANY_PRODUCTS
        )

    private fun product(name: String): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            expirationDate = ExpirationDate(LocalDate.now().plusDays(5)),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

    private companion object {
        val utf8Html: MediaType = MediaType("text", "html", StandardCharsets.UTF_8)
    }
}
