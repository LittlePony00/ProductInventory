package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.barcode

import com.android.rut.miit.productinventory.application.dto.response.barcode.BarcodeProductDraftResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest.currentUserId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/barcodes")
class BarcodeProductDraftController(
    private val barcodeProductService: IBarcodeProductService
) {
    @GetMapping("/{barcode}")
    fun getProductDraft(
        @PathVariable householdId: UUID,
        @PathVariable barcode: String
    ): BarcodeProductDraftResponse =
        barcodeProductService.getProductDraft(currentUserId(), householdId, barcode).toResponse()
}
