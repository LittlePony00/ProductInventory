package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.openfoodfacts

import com.android.rut.miit.productinventory.domain.model.OpenFoodFactsProduct
import com.android.rut.miit.productinventory.domain.port.outbound.IProductInfoProvider
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OpenFoodFactsClient(
    private val restTemplate: RestTemplate
) : IProductInfoProvider {

    private val log = LoggerFactory.getLogger(OpenFoodFactsClient::class.java)

    override fun lookupByBarcode(barcode: String): OpenFoodFactsProduct? {
        return try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
            val response = restTemplate.getForObject(url, JsonNode::class.java) ?: return null

            val status = response.get("status")?.asInt() ?: 0
            if (status != 1) return null

            val product = response.get("product") ?: return null

            OpenFoodFactsProduct(
                name = product.get("product_name")?.asText()?.takeIf { it.isNotBlank() } ?: return null,
                category = product.get("categories")?.asText()?.takeIf { it.isNotBlank() },
                imageUrl = product.get("image_url")?.asText()?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            log.warn("Failed to fetch product info from Open Food Facts for barcode: $barcode", e)
            null
        }
    }
}
