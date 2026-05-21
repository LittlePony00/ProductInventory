package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class Category(
    val id: UUID = UUID.randomUUID(),
    val householdId: UUID? = null,
    val code: ProductCategory? = null,
    val name: String,
    val system: Boolean = householdId == null,
    val archived: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Category name is required" }
        require(system == (householdId == null)) { "System category scope is inconsistent" }
    }
}

object SystemCategoryCatalog {
    val dairyId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")
    val meatFishId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000102")
    val vegetablesFruitsId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000103")
    val cerealsId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000104")
    val beveragesId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000105")
    val otherId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000106")

    val categories: List<Category> = listOf(
        Category(id = dairyId, code = ProductCategory.DAIRY, name = "Молочные продукты"),
        Category(id = meatFishId, code = ProductCategory.MEAT_FISH, name = "Мясо и рыба"),
        Category(id = vegetablesFruitsId, code = ProductCategory.VEGETABLES_FRUITS, name = "Овощи и фрукты"),
        Category(id = cerealsId, code = ProductCategory.CEREALS, name = "Крупы и злаки"),
        Category(id = beveragesId, code = ProductCategory.BEVERAGES, name = "Напитки"),
        Category(id = otherId, code = ProductCategory.OTHER, name = "Другое")
    )

    fun idFor(category: ProductCategory): UUID =
        categories.first { it.code == category }.id
}
