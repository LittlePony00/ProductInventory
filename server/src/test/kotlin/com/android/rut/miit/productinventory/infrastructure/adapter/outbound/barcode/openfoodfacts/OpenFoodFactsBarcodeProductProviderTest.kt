package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.openfoodfacts

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import java.util.UUID
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpenFoodFactsBarcodeProductProviderTest {

    @Test
    fun `maps successful product response to draft`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = OpenFoodFactsBarcodeProductProvider(builder, "https://off.test")

        server.expect(requestTo("https://off.test/api/v2/product/4601234567890.json"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "status": 1,
                      "product": {
                        "product_name": "Milk",
                        "brands": "Brand A, Brand B",
                        "quantity": "1 l",
                        "ingredients_text": "milk",
                        "categories_tags": ["en:dairies"],
                        "nutriments": {
                          "energy-kcal_100g": 60,
                          "proteins_100g": 3.2,
                          "fat_100g": 3.5,
                          "carbohydrates_100g": 4.7
                        }
                      }
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val draft = client.findDraft(context("4601234567890"))

        assertEquals("Milk", draft?.name)
        assertEquals("Brand A", draft?.brand)
        assertEquals(1000.0, draft?.packageQuantity?.value)
        assertEquals(QuantityUnit.MILLILITERS, draft?.packageQuantity?.unit)
        assertEquals(60.0, draft?.nutrition?.caloriesKcal)
        assertEquals(ProductCategory.DAIRY, draft?.category)
        assertEquals(BarcodeProductSource.OPEN_FOOD_FACTS, draft?.source)
        server.verify()
    }

    @Test
    fun `returns null when product is not found`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = OpenFoodFactsBarcodeProductProvider(builder, "https://off.test")

        server.expect(requestTo("https://off.test/api/v2/product/unknown.json"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"status": 0}""", MediaType.APPLICATION_JSON))

        assertNull(client.findDraft(context("unknown")))
        server.verify()
    }

    @Test
    fun `returns null when request fails`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = OpenFoodFactsBarcodeProductProvider(builder, "https://off.test")

        server.expect(requestTo("https://off.test/api/v2/product/4601234567890.json"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError())

        assertNull(client.findDraft(context("4601234567890")))
        server.verify()
    }

    private fun context(barcode: String): BarcodeLookupContext =
        BarcodeLookupContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            barcode = barcode
        )
}
