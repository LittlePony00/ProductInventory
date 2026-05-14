package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import org.springframework.data.jpa.repository.JpaRepository

interface JpaBarcodeProductCacheRepository : JpaRepository<BarcodeProductCacheEntity, String>
