package com.android.rut.miit.productinventory.application.service.barcode

import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestion
import com.android.rut.miit.productinventory.domain.model.barcode.CategorySuggestionSource
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductCacheRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IBarcodeProductProvider
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.IGigaChatCategoryClient
import com.android.rut.miit.productinventory.domain.service.ProductCategoryRuleMatcher
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarcodeProductServiceImpl(
    private val cacheRepository: IBarcodeProductCacheRepository,
    private val membershipRepository: IMembershipRepository,
    providers: List<IBarcodeProductProvider>,
    private val categorySuggestionService: CategorySuggestionService
) : IBarcodeProductService {

    private val providerChain = providers.sortedBy { it.order.ordinal }

    override fun getProductDraft(userId: UUID, householdId: UUID, barcode: String): BarcodeProductDraft {
        val normalizedBarcode = barcode.trim()
        require(normalizedBarcode.isNotBlank()) { "Barcode is required" }

        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")

        val cachedDraft = cacheRepository.findByBarcode(normalizedBarcode)
        if (cachedDraft?.source?.isCacheableGlobalSource() == true) {
            return cachedDraft.copy(source = BarcodeProductSource.LOCAL_CACHE)
        }

        val context = BarcodeLookupContext(
            userId = userId,
            householdId = householdId,
            barcode = normalizedBarcode
        )
        val providerDraft = providerChain.firstNotNullOfOrNull { it.findDraft(context) }
        val draft = providerDraft ?: emptyDraft(normalizedBarcode)

        val categorizedDraft = if (draft.category == null) {
            draft.withCategory(categorySuggestionService.suggestCategory(draft))
        } else {
            draft
        }

        return if (providerDraft != null && categorizedDraft.source.isCacheableGlobalSource()) {
            cacheRepository.save(categorizedDraft)
        } else {
            categorizedDraft
        }
    }

    private fun emptyDraft(barcode: String): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = barcode,
            name = null,
            brand = null,
            packageQuantity = null,
            ingredients = null,
            imageUrl = null,
            nutrition = null,
            category = null,
            source = BarcodeProductSource.LOCAL_DATABASE,
            confidence = 0.2
        )
}

private fun BarcodeProductSource.isCacheableGlobalSource(): Boolean =
    this == BarcodeProductSource.OPEN_FOOD_FACTS ||
        this == BarcodeProductSource.GS1 ||
        this == BarcodeProductSource.LOCAL_DATABASE

@Service
class CategorySuggestionService(
    private val gigaChatClient: IGigaChatCategoryClient,
    private val ruleMatcher: ProductCategoryRuleMatcher
) {
    fun suggestCategory(draft: BarcodeProductDraft): CategorySuggestion =
        ruleMatcher.suggestCategory(draft.toProductEnrichmentInput())
            ?.let { CategorySuggestion(it.category, it.confidence, CategorySuggestionSource.RULE_BASED) }
            ?: gigaChatClient.suggestCategory(draft)
            ?: CategorySuggestion(ProductCategory.OTHER, 0.2, CategorySuggestionSource.FALLBACK)
}

private fun BarcodeProductDraft.toProductEnrichmentInput() =
    com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput(
        name = name,
        brand = brand,
        barcode = barcode,
        ingredientsText = ingredients
    )
