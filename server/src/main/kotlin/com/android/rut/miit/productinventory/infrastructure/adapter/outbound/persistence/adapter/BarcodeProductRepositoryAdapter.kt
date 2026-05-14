package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.BarcodeProduct
import com.android.rut.miit.productinventory.domain.port.outbound.IBarcodeProductRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaBarcodeProductRepository
import org.springframework.stereotype.Component

@Component
class BarcodeProductRepositoryAdapter(
    private val jpaRepository: JpaBarcodeProductRepository
) : IBarcodeProductRepository {

    override fun findByBarcode(barcode: String): BarcodeProduct? =
        jpaRepository.findByBarcode(barcode)?.toDomain()

    override fun save(product: BarcodeProduct): BarcodeProduct =
        jpaRepository.save(product.toEntity()).toDomain()
}
