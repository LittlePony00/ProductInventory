package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.ProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.UUID

interface JpaProductRepository : JpaRepository<ProductEntity, UUID> {
    fun findByHouseholdId(householdId: UUID): List<ProductEntity>
    fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<ProductEntity>
    fun findByHouseholdIdAndExpirationDateBefore(householdId: UUID, date: LocalDate): List<ProductEntity>
    fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): ProductEntity?

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.expirationDate >= :startInclusive AND p.expirationDate < :endExclusive
        """
    )
    fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.householdId = :householdId
          AND p.expirationDate >= :startInclusive
          AND p.expirationDate < :endExclusive
        """
    )
    fun findExpiringBetweenByHouseholdId(
        householdId: UUID,
        startInclusive: LocalDate,
        endExclusive: LocalDate
    ): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.lowStockThreshold IS NOT NULL
          AND p.remainingAmount <= p.lowStockThreshold
        """
    )
    fun findLowStock(): List<ProductEntity>

    @Query(
        """
        SELECT p FROM ProductEntity p
        WHERE p.householdId = :householdId
          AND p.lowStockThreshold IS NOT NULL
          AND p.remainingAmount <= p.lowStockThreshold
        """
    )
    fun findLowStockByHouseholdId(householdId: UUID): List<ProductEntity>
}
