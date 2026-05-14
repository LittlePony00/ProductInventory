package com.android.rut.miit.productinventory.domain.port.outbound.barcode

import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion

interface IGigaChatCategoryClient {
    fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion?
}
