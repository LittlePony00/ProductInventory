package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.Product
import java.time.LocalDate
import java.util.UUID

interface IProductRepository {
    fun findById(id: UUID): Product?
    fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product?
    fun findByHouseholdId(householdId: UUID): List<Product>
    fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product>
    fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product>
    fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product>
    fun findExpiringBetweenByHouseholdId(
        householdId: UUID,
        startInclusive: LocalDate,
        endExclusive: LocalDate
    ): List<Product>
    fun findLowStock(): List<Product>
    fun findLowStockByHouseholdId(householdId: UUID): List<Product>
    fun save(product: Product): Product
    fun deleteById(id: UUID)
    fun existsById(id: UUID): Boolean
}
