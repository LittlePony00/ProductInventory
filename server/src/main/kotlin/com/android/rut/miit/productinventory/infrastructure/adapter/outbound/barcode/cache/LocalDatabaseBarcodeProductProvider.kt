package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
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

    override fun findDraft(barcode: String): BarcodeProductDraft? =
        productRepository.findFirstByBarcode(barcode)?.let { product ->
            BarcodeProductDraft(
                barcode = barcode,
                name = product.name,
                brand = product.brand,
                packageQuantity = Quantity(product.quantity.value, product.quantity.unit),
                ingredients = product.ingredients,
                nutrition = NutritionFacts(
                    caloriesKcal = product.caloriesKcal,
                    proteinGrams = product.proteinGrams,
                    fatGrams = product.fatGrams,
                    carbohydratesGrams = product.carbohydratesGrams
                ),
                category = product.category,
                source = BarcodeProductSource.LOCAL_DATABASE,
                confidence = 0.72
            )
        }
}
