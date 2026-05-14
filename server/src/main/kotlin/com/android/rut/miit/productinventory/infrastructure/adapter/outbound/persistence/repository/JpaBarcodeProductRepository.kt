package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository

import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.BarcodeProductEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaBarcodeProductRepository : JpaRepository<BarcodeProductEntity, UUID> {
    fun findByBarcode(barcode: String): BarcodeProductEntity?
}
