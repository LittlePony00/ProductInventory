package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.data.local.dao.BarcodeDao
import com.android.rut.miit.productinventory.data.local.entity.BarcodeEntity

class RoomBarcodeLocalDataSource(
    private val barcodeDao: BarcodeDao
) : BarcodeLocalDataSource {

    override suspend fun getCachedBarcode(code: String): CachedBarcodeProduct? {
        return barcodeDao.getByBarcode(code)?.toDomain()
    }

    override suspend fun saveBarcode(product: CachedBarcodeProduct) {
        barcodeDao.insert(product.toEntity())
    }

    override suspend fun isBarcodeKnown(code: String): Boolean {
        return barcodeDao.exists(code)
    }

    private fun BarcodeEntity.toDomain() = CachedBarcodeProduct(
        barcode = barcode,
        name = name,
        category = category,
        imageUrl = imageUrl
    )

    private fun CachedBarcodeProduct.toEntity() = BarcodeEntity(
        barcode = barcode,
        name = name,
        category = category,
        imageUrl = imageUrl
    )
}
