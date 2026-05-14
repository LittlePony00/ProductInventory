package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals

@DataJpaTest
@Import(BarcodeProductCacheRepositoryAdapter::class)
class BarcodeProductCacheRepositoryAdapterTest(
    @param:Autowired private val adapter: BarcodeProductCacheRepositoryAdapter
) {

    @Test
    fun `saves and reads cached barcode draft`() {
        val draft = BarcodeProductDraft(
            barcode = "4601234567890",
            name = "Milk",
            brand = "Brand",
            packageQuantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
            ingredients = "milk",
            nutrition = NutritionFacts(60.0, 3.2, 3.5, 4.7),
            category = ProductCategory.DAIRY,
            source = BarcodeProductSource.OPEN_FOOD_FACTS,
            confidence = 0.86
        )

        adapter.save(draft)
        val cached = adapter.findByBarcode("4601234567890")

        assertEquals(draft.barcode, cached?.barcode)
        assertEquals(draft.name, cached?.name)
        assertEquals(draft.packageQuantity, cached?.packageQuantity)
        assertEquals(draft.nutrition, cached?.nutrition)
        assertEquals(draft.category, cached?.category)
        assertEquals(draft.source, cached?.source)
        assertEquals(draft.confidence, cached?.confidence)
    }
}
