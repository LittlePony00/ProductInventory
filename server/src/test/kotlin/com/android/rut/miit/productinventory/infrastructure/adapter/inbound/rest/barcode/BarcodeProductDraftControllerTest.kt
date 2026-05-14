package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import java.util.UUID
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.assertEquals

class BarcodeProductDraftControllerTest {

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `returns barcode product draft response from household endpoint`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = RecordingBarcodeProductService()
        val controller = BarcodeProductDraftController(service)
        val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        authenticate(userId)

        mockMvc.get("/api/v1/households/$householdId/barcodes/4601234567890")
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

        assertEquals(userId, service.lastUserId)
        assertEquals(householdId, service.lastHouseholdId)
        assertEquals("4601234567890", service.lastBarcode)
    }

    @Test
    fun `old global endpoint is not mapped`() {
        val controller = BarcodeProductDraftController(RecordingBarcodeProductService())
        val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        authenticate(UUID.randomUUID())

        mockMvc.get("/api/v1/barcodes/4601234567890")
            .andExpect {
                status { isNotFound() }
            }
    }

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }
}

private class RecordingBarcodeProductService : IBarcodeProductService {
    var lastUserId: UUID? = null
        private set
    var lastHouseholdId: UUID? = null
        private set
    var lastBarcode: String? = null
        private set

    override fun getProductDraft(userId: UUID, householdId: UUID, barcode: String): BarcodeProductDraft {
        lastUserId = userId
        lastHouseholdId = householdId
        lastBarcode = barcode
        return BarcodeProductDraft(
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
}
