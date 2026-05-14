package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.barcode

import com.android.rut.miit.productinventory.application.dto.response.barcode.BarcodeProductDraftResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/barcodes")
class BarcodeController(
    private val barcodeProductService: IBarcodeProductService
) {
    @GetMapping("/{barcode}")
    fun getProductDraft(@PathVariable barcode: String): BarcodeProductDraftResponse =
        barcodeProductService.getProductDraft(barcode).toResponse()
}
