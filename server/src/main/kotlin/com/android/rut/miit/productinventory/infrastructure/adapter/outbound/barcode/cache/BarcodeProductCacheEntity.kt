package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "barcode_product_cache")
class BarcodeProductCacheEntity(
    @Id
    val barcode: String = "",

    var name: String? = null,
    var brand: String? = null,

    @Column(name = "package_quantity")
    var packageQuantity: Double? = null,

    @Column(name = "package_quantity_unit")
    var packageQuantityUnit: String? = null,

    @Column(columnDefinition = "TEXT")
    var ingredients: String? = null,

    @Column(name = "calories_kcal")
    var caloriesKcal: Double? = null,

    @Column(name = "protein_grams")
    var proteinGrams: Double? = null,

    @Column(name = "fat_grams")
    var fatGrams: Double? = null,

    @Column(name = "carbohydrates_grams")
    var carbohydratesGrams: Double? = null,

    var category: String? = null,

    @Column(nullable = false)
    var source: String = "LOCAL_CACHE",

    @Column(nullable = false)
    var confidence: Double = 0.0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
