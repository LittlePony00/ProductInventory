package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.AiProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentCategoryOption
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput

interface IProductEnrichmentClient {
    fun suggestProduct(
        input: ProductEnrichmentInput,
        categories: List<ProductEnrichmentCategoryOption>
    ): AiProductEnrichmentSuggestion?
}
