package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class BarcodeControllerTest {

    @Test
    fun `returns barcode product draft response`() {
        val controller = BarcodeController(
            object : IBarcodeProductService {
                override fun getProductDraft(barcode: String): BarcodeProductDraft =
                    BarcodeProductDraft(
                        barcode = barcode,
                        name = "Milk",
                        brand = "Brand",
                        packageQuantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
                        ingredients = "milk",
                        nutrition = NutritionFacts(60.0, 3.2, 3.5, 4.7),
                        category = ProductCategory.DAIRY,
                        source = BarcodeProductSource.OPEN_FOOD_FACTS,
                        confidence = 0.86
                    )
            }
        )
        val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()

        mockMvc.get("/api/v1/barcodes/4601234567890")
            .andExpect {
                status { isOk() }
                jsonPath("$.barcode", equalTo("4601234567890"))
                jsonPath("$.name", equalTo("Milk"))
                jsonPath("$.brand", equalTo("Brand"))
                jsonPath("$.packageQuantity", equalTo(1000.0))
                jsonPath("$.packageQuantityUnit", equalTo("MILLILITERS"))
                jsonPath("$.caloriesKcal", equalTo(60.0))
                jsonPath("$.category", equalTo("DAIRY"))
                jsonPath("$.source", equalTo("OPEN_FOOD_FACTS"))
                jsonPath("$.confidence", equalTo(0.86))
            }
    }
}
