package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaProductRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class ProductRepositoryAdapter(
    private val jpaRepository: JpaProductRepository
) : IProductRepository {

    override fun findById(id: UUID): Product? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
        jpaRepository.findByBarcodeAndHouseholdId(barcode, householdId)?.toDomain()

    override fun findByHouseholdId(householdId: UUID): List<Product> =
        jpaRepository.findByHouseholdId(householdId).map { it.toDomain() }

    override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
        jpaRepository.findByHouseholdIdAndCategoryId(householdId, categoryId).map { it.toDomain() }

    override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> =
        jpaRepository.findByHouseholdIdAndExpirationDateBefore(householdId, date).map { it.toDomain() }

    override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
        jpaRepository.findExpiringBetween(startInclusive, endExclusive).map { it.toDomain() }

    override fun findExpiringBetweenByHouseholdId(
        householdId: UUID,
        startInclusive: LocalDate,
        endExclusive: LocalDate
    ): List<Product> =
        jpaRepository.findExpiringBetweenByHouseholdId(householdId, startInclusive, endExclusive)
            .map { it.toDomain() }

    override fun findLowStock(): List<Product> =
        jpaRepository.findLowStock().map { it.toDomain() }

    override fun findLowStockByHouseholdId(householdId: UUID): List<Product> =
        jpaRepository.findLowStockByHouseholdId(householdId).map { it.toDomain() }

    override fun save(product: Product): Product =
        jpaRepository.save(product.toEntity()).toDomain()

    override fun deleteById(id: UUID) =
        jpaRepository.deleteById(id)

    override fun existsById(id: UUID): Boolean =
        jpaRepository.existsById(id)
}
