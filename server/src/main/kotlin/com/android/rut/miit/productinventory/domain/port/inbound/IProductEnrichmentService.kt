package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentSuggestion
import java.util.UUID

interface IProductEnrichmentService {
    fun suggestProduct(
        userId: UUID,
        householdId: UUID,
        input: ProductEnrichmentInput
    ): ProductEnrichmentSuggestion
}
