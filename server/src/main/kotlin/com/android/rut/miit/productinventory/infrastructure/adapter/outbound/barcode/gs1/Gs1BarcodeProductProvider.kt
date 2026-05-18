package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.gs1

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeProductProviderOrder
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@Order(200)
class Gs1BarcodeProductProvider(
    restClientBuilder: RestClient.Builder,
    @Value("\${barcode.gs1.base-url:}") baseUrl: String
) : IBarcodeProductProvider {
    override val order: BarcodeProductProviderOrder = BarcodeProductProviderOrder.GS1

    private val restClient = baseUrl.takeIf(String::isNotBlank)?.let {
        restClientBuilder.baseUrl(it).build()
    }

    override fun findDraft(context: BarcodeLookupContext): BarcodeProductDraft? {
        val client = restClient ?: return null
        return try {
            client.get()
                .uri("/products/{barcode}", context.barcode)
                .retrieve()
                .body(Gs1ProductResponse::class.java)
                ?.toDraft(context.barcode)
        } catch (_: RestClientException) {
            null
        }
    }
}

private fun Gs1ProductResponse.toDraft(barcode: String): BarcodeProductDraft =
    BarcodeProductDraft(
        barcode = barcode,
        name = name?.takeIf(String::isNotBlank),
        brand = brand?.takeIf(String::isNotBlank),
        packageQuantity = null,
        ingredients = null,
        imageUrl = imageUrl?.takeIf(String::isNotBlank),
        nutrition = null,
        category = null,
        source = BarcodeProductSource.GS1,
        confidence = if (name.isNullOrBlank()) 0.5 else 0.78
    )

@JsonIgnoreProperties(ignoreUnknown = true)
data class Gs1ProductResponse(
    val name: String? = null,
    val brand: String? = null,
    val imageUrl: String? = null
)
