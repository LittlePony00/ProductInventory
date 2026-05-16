package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.BarcodeLookupRequest
import com.android.rut.miit.productinventory.application.dto.response.ProductResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BarcodeController(
    private val barcodeService: IBarcodeService
) {

    @PostMapping("/households/{householdId}/products/barcode")
    @ResponseStatus(HttpStatus.CREATED)
    fun lookupAndAddProduct(
        @PathVariable householdId: UUID,
        @Valid @RequestBody request: BarcodeLookupRequest
    ): ProductResponse {
        return barcodeService.lookupAndAddProduct(
            householdId = householdId.toString(),
            userId = currentUserId().toString(),
            barcode = request.barcode
        )
    }
}
