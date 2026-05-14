package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.gs1

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

class Gs1BarcodeProductProviderTest {

    @Test
    fun `maps gs1 product response to draft`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = Gs1BarcodeProductProvider(builder, "https://gs1.test")

        server.expect(requestTo("https://gs1.test/products/4601234567890"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"name":"Milk","brand":"Brand"}""", MediaType.APPLICATION_JSON))

        val draft = provider.findDraft(context("4601234567890"))

        assertEquals("Milk", draft?.name)
        assertEquals("Brand", draft?.brand)
        assertEquals(BarcodeProductSource.GS1, draft?.source)
        server.verify()
    }

    @Test
    fun `returns null when gs1 base url is not configured`() {
        val provider = Gs1BarcodeProductProvider(RestClient.builder(), "")

        assertNull(provider.findDraft(context("4601234567890")))
    }

    @Test
    fun `returns null when gs1 request fails`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val provider = Gs1BarcodeProductProvider(builder, "https://gs1.test")

        server.expect(requestTo("https://gs1.test/products/4601234567890"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withServerError())

        assertNull(provider.findDraft(context("4601234567890")))
        server.verify()
    }

    private fun context(barcode: String): BarcodeLookupContext =
        BarcodeLookupContext(
            userId = UUID.randomUUID(),
            householdId = UUID.randomUUID(),
            barcode = barcode
        )
}
