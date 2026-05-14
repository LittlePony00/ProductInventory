package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeProductProviderOrder
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(300)
class LocalDatabaseBarcodeProductProvider(
    private val productRepository: IProductRepository
) : IBarcodeProductProvider {
    override val order: BarcodeProductProviderOrder = BarcodeProductProviderOrder.LOCAL_DATABASE

    override fun findDraft(context: BarcodeLookupContext): BarcodeProductDraft? =
        productRepository.findByBarcodeAndHouseholdId(context.barcode, context.householdId)?.let { product ->
            BarcodeProductDraft(
                barcode = context.barcode,
                name = product.name,
                brand = product.brand,
                packageQuantity = product.packageQuantity ?: Quantity(product.quantity.value, product.quantity.unit),
                ingredients = product.ingredientsText,
                nutrition = NutritionFacts(
                    caloriesKcal = product.calories,
                    proteinGrams = product.protein,
                    fatGrams = product.fat,
                    carbohydratesGrams = product.carbs
                ),
                category = product.category,
                source = BarcodeProductSource.LOCAL_DATABASE,
                confidence = 0.72
            )
        }
}
