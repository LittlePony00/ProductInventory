package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import org.springframework.stereotype.Component

@Component
class BarcodeProductCacheRepositoryAdapter(
    private val jpaRepository: JpaBarcodeProductCacheRepository
) : IBarcodeProductCacheRepository {

    override fun findByBarcode(barcode: String): BarcodeProductDraft? =
        jpaRepository.findById(barcode).orElse(null)?.toDomain()

    override fun save(draft: BarcodeProductDraft): BarcodeProductDraft =
        jpaRepository.save(draft.toEntity()).toDomain()
}

private fun BarcodeProductCacheEntity.toDomain(): BarcodeProductDraft =
    BarcodeProductDraft(
        barcode = barcode,
        name = name,
        brand = brand,
        packageQuantity = packageQuantityUnit?.let(QuantityUnit::valueOf)?.let { unit ->
            packageQuantity?.let { Quantity(it, unit) }
        },
        ingredients = ingredients,
        imageUrl = imageUrl,
        nutrition = NutritionFacts(caloriesKcal, proteinGrams, fatGrams, carbohydratesGrams),
        category = category?.let(ProductCategory::valueOf),
        source = BarcodeProductSource.valueOf(source),
        confidence = confidence
    )

private fun BarcodeProductDraft.toEntity(): BarcodeProductCacheEntity =
    BarcodeProductCacheEntity(
        barcode = barcode,
        name = name,
        brand = brand,
        packageQuantity = packageQuantity?.value,
        packageQuantityUnit = packageQuantity?.unit?.name,
        ingredients = ingredients,
        imageUrl = imageUrl,
        caloriesKcal = nutrition?.caloriesKcal,
        proteinGrams = nutrition?.proteinGrams,
        fatGrams = nutrition?.fatGrams,
        carbohydratesGrams = nutrition?.carbohydratesGrams,
        category = category?.name,
        source = source.name,
        confidence = confidence
    )
