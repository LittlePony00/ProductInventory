package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.openfoodfacts

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeProductProviderOrder
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
@Order(100)
class OpenFoodFactsClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${barcode.open-food-facts.base-url:https://world.openfoodfacts.org}") baseUrl: String
) : IBarcodeProductProvider {
    override val order: BarcodeProductProviderOrder = BarcodeProductProviderOrder.OPEN_FOOD_FACTS

    private val restClient = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeader("User-Agent", "ProductInventory/1.0 backend")
        .build()

    override fun findDraft(barcode: String): BarcodeProductDraft? =
        try {
            restClient.get()
                .uri("/api/v2/product/{barcode}.json", barcode)
                .retrieve()
                .body(OpenFoodFactsResponse::class.java)
                ?.toDraft(barcode)
        } catch (_: RestClientException) {
            null
        }
}

private fun OpenFoodFactsResponse.toDraft(barcode: String): BarcodeProductDraft? {
    if (status != 1 || product == null) return null

    val quantity = product.quantity?.toQuantity()
    val category = product.categoriesTags.orEmpty()
        .plus(product.categories.orEmpty())
        .firstNotNullOfOrNull(::mapCategory)

    return BarcodeProductDraft(
        barcode = barcode,
        name = product.productName?.takeIf(String::isNotBlank),
        brand = product.brands?.split(",")?.firstOrNull()?.trim()?.takeIf(String::isNotBlank),
        packageQuantity = quantity,
        ingredients = product.ingredientsText?.takeIf(String::isNotBlank),
        nutrition = product.nutriments?.toNutritionFacts(),
        category = category,
        source = BarcodeProductSource.OPEN_FOOD_FACTS,
        confidence = if (product.productName.isNullOrBlank()) 0.55 else 0.86
    )
}

private fun String.toQuantity(): Quantity? {
    val match = QUANTITY_REGEX.find(lowercase()) ?: return null
    val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val unit = when (match.groupValues[2]) {
        "g", "гр", "г" -> QuantityUnit.GRAMS
        "kg", "кг" -> QuantityUnit.GRAMS
        "ml", "мл" -> QuantityUnit.MILLILITERS
        "l", "л" -> QuantityUnit.MILLILITERS
        else -> QuantityUnit.PIECES
    }
    val normalizedValue = when (match.groupValues[2]) {
        "kg", "кг", "l", "л" -> value * 1000
        else -> value
    }
    return Quantity(normalizedValue, unit)
}

private fun mapCategory(value: String): ProductCategory? {
    val normalized = value.lowercase()
    return when {
        listOf("dair", "milk", "cheese", "yogurt", "мол").any(normalized::contains) -> ProductCategory.DAIRY
        listOf("meat", "fish", "seafood", "мяс", "рыб").any(normalized::contains) -> ProductCategory.MEAT_FISH
        listOf("fruit", "vegetable", "овощ", "фрукт").any(normalized::contains) -> ProductCategory.VEGETABLES_FRUITS
        listOf("cereal", "bread", "pasta", "rice", "круп", "хлеб").any(normalized::contains) -> ProductCategory.CEREALS
        listOf("beverage", "drink", "water", "juice", "напит", "вода", "сок").any(normalized::contains) -> ProductCategory.BEVERAGES
        else -> null
    }
}

private fun Nutriments.toNutritionFacts(): NutritionFacts =
    NutritionFacts(
        caloriesKcal = energyKcal100g,
        proteinGrams = proteins100g,
        fatGrams = fat100g,
        carbohydratesGrams = carbohydrates100g
    )

private val QUANTITY_REGEX = """(\d+(?:[\.,]\d+)?)\s*(kg|кг|g|гр|г|ml|мл|l|л|pcs|шт)""".toRegex()

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenFoodFactsResponse(
    val status: Int = 0,
    val product: OpenFoodFactsProduct? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenFoodFactsProduct(
    @param:JsonProperty("product_name")
    val productName: String? = null,
    val brands: String? = null,
    val quantity: String? = null,
    @param:JsonProperty("ingredients_text")
    val ingredientsText: String? = null,
    val categories: String? = null,
    @param:JsonProperty("categories_tags")
    val categoriesTags: List<String>? = null,
    val nutriments: Nutriments? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Nutriments(
    @param:JsonProperty("energy-kcal_100g")
    val energyKcal100g: Double? = null,
    @param:JsonProperty("proteins_100g")
    val proteins100g: Double? = null,
    @param:JsonProperty("fat_100g")
    val fat100g: Double? = null,
    @param:JsonProperty("carbohydrates_100g")
    val carbohydrates100g: Double? = null
)
