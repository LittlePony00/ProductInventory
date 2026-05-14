package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.application.dto.response.BarcodeProductResponse
import com.android.rut.miit.productinventory.application.dto.response.ProductResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.exception.BarcodeNotFoundException
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeService
import com.android.rut.miit.productinventory.domain.port.outbound.IBarcodeProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductInfoProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BarcodeServiceImpl(
    private val barcodeProductRepository: IBarcodeProductRepository,
    private val productInfoProvider: IProductInfoProvider,
    private val productRepository: IProductRepository
) : IBarcodeService {

    @Transactional
    override fun lookupAndAddProduct(householdId: String, userId: String, barcode: String): ProductResponse {
        val cached = barcodeProductRepository.findByBarcode(barcode)

        val barcodeProduct = cached ?: run {
            val offProduct = productInfoProvider.lookupByBarcode(barcode)
                ?: throw BarcodeNotFoundException(barcode)

            barcodeProductRepository.save(
                BarcodeProduct(
                    barcode = barcode,
                    name = offProduct.name,
                    category = offProduct.category,
                    imageUrl = offProduct.imageUrl
                )
            )
        }

        val product = productRepository.save(
            Product(
                name = barcodeProduct.name,
                category = ProductCategory.OTHER,
                quantity = Quantity(value = 1.0, unit = QuantityUnit.PIECES),
                householdId = UUID.fromString(householdId),
                addedByUserId = UUID.fromString(userId),
                barcode = barcode
            )
        )

        return product.toResponse()
    }

    @Transactional(readOnly = true)
    override fun lookupBarcode(barcode: String): BarcodeProductResponse? {
        val cached = barcodeProductRepository.findByBarcode(barcode)
        if (cached != null) {
            return BarcodeProductResponse(
                barcode = cached.barcode,
                name = cached.name,
                category = cached.category,
                imageUrl = cached.imageUrl
            )
        }

        val offProduct = productInfoProvider.lookupByBarcode(barcode) ?: return null

        return BarcodeProductResponse(
            barcode = barcode,
            name = offProduct.name,
            category = offProduct.category,
            imageUrl = offProduct.imageUrl
        )
    }
}
