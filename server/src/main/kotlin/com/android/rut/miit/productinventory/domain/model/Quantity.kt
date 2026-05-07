package com.android.rut.miit.productinventory.domain.model

data class Quantity(
    val value: Double,
    val unit: QuantityUnit
) {
    init {
        require(value >= 0) { "Quantity value must be non-negative" }
    }

    operator fun plus(other: Quantity): Quantity {
        require(unit == other.unit) { "Cannot add quantities with different units" }
        return copy(value = value + other.value)
    }

    operator fun minus(other: Quantity): Quantity {
        require(unit == other.unit) { "Cannot subtract quantities with different units" }
        return copy(value = (value - other.value).coerceAtLeast(0.0))
    }
}
