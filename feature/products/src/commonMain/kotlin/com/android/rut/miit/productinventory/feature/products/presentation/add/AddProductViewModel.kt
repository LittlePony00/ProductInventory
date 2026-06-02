package com.android.rut.miit.productinventory.feature.products.presentation.add

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.SuggestProductEnrichmentUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class AddProductViewModel(
    private val addProductUseCase: AddProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val getProductUseCase: GetProductUseCase,
    private val getProductCategoriesUseCase: GetProductCategoriesUseCase,
    private val createProductCategoryUseCase: CreateProductCategoryUseCase,
    private val suggestProductEnrichmentUseCase: SuggestProductEnrichmentUseCase
) : SharedViewModel<AddProductState, AddProductEvent, AddProductAction>(
    initialState = AddProductState()
) {

    var householdId: String = ""
    private var productId: String? = null

    override suspend fun handleEvent(event: AddProductEvent) {
        when (event) {
            is AddProductEvent.OnCreate -> onCreate(event.householdId)
            is AddProductEvent.OnLoadProduct -> loadProduct(event.productId)
            is AddProductEvent.OnPrefill -> prefill(event)
            is AddProductEvent.OnScannedDraftApplied -> applyScannedDraft(event)
            is AddProductEvent.OnNameChanged ->
                updateState { copy(name = event.name) }
            is AddProductEvent.OnBrandChanged ->
                updateState { copy(brand = event.brand) }
            is AddProductEvent.OnBarcodeChanged ->
                updateState { copy(barcode = event.barcode) }
            is AddProductEvent.OnCategoryChanged ->
                updateState { copy(categoryId = event.categoryId, category = event.category) }
            is AddProductEvent.OnNewCategoryNameChanged ->
                updateState { copy(newCategoryName = event.name) }
            is AddProductEvent.OnCreateCategoryClick -> createCategory()
            is AddProductEvent.OnSuggestProductClick -> suggestProduct()
            is AddProductEvent.OnQuantityChanged ->
                updateState { copy(quantity = event.quantity) }
            is AddProductEvent.OnQuantityUnitChanged ->
                updateState { copy(quantityUnit = event.unit) }
            is AddProductEvent.OnRemainingAmountChanged ->
                updateState { copy(remainingAmount = event.amount) }
            is AddProductEvent.OnLowStockThresholdChanged ->
                updateState { copy(lowStockThreshold = event.threshold) }
            is AddProductEvent.OnExpirationDateChanged ->
                updateState { copy(expirationDate = event.date) }
            is AddProductEvent.OnPackageAmountChanged ->
                updateState { copy(packageAmount = event.amount) }
            is AddProductEvent.OnPackageUnitChanged ->
                updateState { copy(packageUnit = event.unit) }
            is AddProductEvent.OnIngredientsChanged ->
                updateState { copy(ingredientsText = event.ingredients) }
            is AddProductEvent.OnImageSelected ->
                updateState {
                    copy(
                        imageUrl = null,
                        localImagePath = event.localImagePath,
                        isImageRemoved = false,
                        isImageChanged = true
                    )
                }
            is AddProductEvent.OnImageRemoved ->
                updateState {
                    copy(
                        localImagePath = null,
                        imageUrl = null,
                        isImageRemoved = true,
                        isImageChanged = true
                    )
                }
            is AddProductEvent.OnCaloriesChanged ->
                updateState { copy(calories = event.calories) }
            is AddProductEvent.OnProteinChanged ->
                updateState { copy(protein = event.protein) }
            is AddProductEvent.OnFatChanged ->
                updateState { copy(fat = event.fat) }
            is AddProductEvent.OnCarbsChanged ->
                updateState { copy(carbs = event.carbs) }
            is AddProductEvent.OnSaveClick -> save()
            is AddProductEvent.OnBackClick ->
                sendAction(AddProductAction.NavigateBack)
        }
    }

    private fun onCreate(householdId: String) {
        this.householdId = householdId
        productId = null
        updateState { AddProductState() }
        viewModelScope.launch {
            runCatching { getProductCategoriesUseCase(householdId) }
                .onSuccess { categories ->
                    updateState {
                        val selectedId = categoryId ?: categories.idFor(category)
                        copy(categories = categories, categoryId = selectedId)
                    }
                }
                .onFailure {
                    updateState {
                        val fallback = ProductCategoryOption.systemDefaults()
                        copy(categories = fallback, categoryId = categoryId ?: fallback.idFor(category))
                    }
                }
        }
    }

    private fun prefill(event: AddProductEvent.OnPrefill) {
        updateState {
            copy(
                barcode = event.barcode ?: barcode,
                name = event.name ?: name,
                brand = event.brand ?: brand,
                category = event.category ?: category,
                categoryId = event.category?.let { categories.idFor(it) } ?: categoryId,
                quantity = event.quantity ?: quantity,
                quantityUnit = event.quantityUnit ?: quantityUnit,
                packageAmount = event.packageAmount ?: packageAmount,
                packageUnit = event.packageUnit ?: packageUnit,
                ingredientsText = event.ingredientsText ?: ingredientsText,
                imageUrl = event.imageUrl ?: imageUrl,
                localImagePath = event.localImagePath ?: localImagePath,
                calories = event.calories ?: calories,
                protein = event.protein ?: protein,
                fat = event.fat ?: fat,
                carbs = event.carbs ?: carbs,
                isBarcodePrefilled = event.barcode != null || isBarcodePrefilled
            )
        }
    }

    private fun loadProduct(productId: String) {
        if (this.productId == productId) return
        this.productId = productId
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching { getProductUseCase(householdId, productId) }
                .onSuccess { product ->
                    updateState { fromProduct(product).copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    sendAction(AddProductAction.ShowError(error.message ?: "Ошибка"))
                }
        }
    }

    private fun applyScannedDraft(event: AddProductEvent.OnScannedDraftApplied) {
        updateState {
            copy(
                barcode = event.barcode,
                name = event.name ?: name,
                brand = event.brand ?: brand,
                category = event.category ?: category,
                categoryId = event.category?.let { categories.idFor(it) } ?: categoryId,
                packageAmount = event.packageAmount ?: packageAmount,
                packageUnit = event.packageUnit ?: packageUnit,
                ingredientsText = event.ingredientsText ?: ingredientsText,
                imageUrl = event.imageUrl ?: imageUrl,
                localImagePath = event.localImagePath ?: localImagePath,
                calories = event.calories ?: calories,
                protein = event.protein ?: protein,
                fat = event.fat ?: fat,
                carbs = event.carbs ?: carbs,
                isBarcodePrefilled = true
            )
        }
    }

    private fun createCategory() {
        val name = currentState.newCategoryName.trim()
        if (name.isBlank()) {
            updateState { copy(error = "Введите название категории") }
            sendAction(AddProductAction.ShowError("Введите название категории"))
            return
        }
        viewModelScope.launch {
            updateState { copy(isCreatingCategory = true, error = null) }
            runCatching { createProductCategoryUseCase(householdId, name) }
                .onSuccess { category ->
                    updateState {
                        copy(
                            categories = (categories.filterNot { it.id == category.id } + category)
                                .sortedWith(compareBy<ProductCategoryOption> { !it.system }.thenBy { it.name.lowercase() }),
                            categoryId = category.id,
                            category = category.legacyCategory,
                            newCategoryName = "",
                            isCreatingCategory = false
                        )
                    }
                }
                .onFailure { error ->
                    updateState { copy(isCreatingCategory = false, error = error.message) }
                    sendAction(AddProductAction.ShowError(error.message ?: "Ошибка"))
                }
        }
    }

    private fun suggestProduct() {
        val state = currentState
        if (listOf(state.name, state.brand, state.barcode, state.ingredientsText).all { it.isBlank() }) {
            updateState { copy(error = "Введите данные продукта для подсказки") }
            sendAction(AddProductAction.ShowError("Введите данные продукта для подсказки"))
            return
        }

        viewModelScope.launch {
            updateState { copy(isSuggestingProduct = true, error = null, suggestionMessage = null) }
            runCatching {
                suggestProductEnrichmentUseCase(
                    householdId = householdId,
                    name = state.name.trim().ifBlank { null },
                    brand = state.brand.trim().ifBlank { null },
                    barcode = state.barcode.trim().ifBlank { null },
                    ingredientsText = state.ingredientsText.trim().ifBlank { null }
                )
            }.onSuccess { suggestion ->
                applySuggestion(suggestion)
            }.onFailure { error ->
                updateState { copy(isSuggestingProduct = false, error = error.message) }
                sendAction(AddProductAction.ShowError(error.message ?: "Ошибка подсказки"))
            }
        }
    }

    private fun applySuggestion(suggestion: ProductEnrichmentSuggestion) {
        updateState {
            copy(
                category = suggestion.category,
                categoryId = suggestion.categoryId,
                name = name.ifBlank { suggestion.suggestedName.orEmpty() },
                brand = brand.ifBlank { suggestion.suggestedBrand.orEmpty() },
                ingredientsText = ingredientsText.ifBlank { suggestion.suggestedIngredientsText.orEmpty() },
                calories = calories.ifBlank { suggestion.calories?.formatNumber().orEmpty() },
                protein = protein.ifBlank { suggestion.protein?.formatNumber().orEmpty() },
                fat = fat.ifBlank { suggestion.fat?.formatNumber().orEmpty() },
                carbs = carbs.ifBlank { suggestion.carbs?.formatNumber().orEmpty() },
                isSuggestingProduct = false,
                suggestionMessage = suggestion.message()
            )
        }
    }

    private fun save() {
        val state = currentState
        if (state.name.isBlank()) {
            updateState { copy(error = "Введите название продукта") }
            sendAction(AddProductAction.ShowError("Введите название продукта"))
            return
        }
        val qty = state.quantity.toDoubleOrNull()
        if (qty == null || qty <= 0) {
            updateState { copy(error = "Введите корректное количество") }
            sendAction(AddProductAction.ShowError("Введите корректное количество"))
            return
        }

        val expDate = when (val parsed = state.expirationDate.parseOptionalDate("Введите срок годности в формате ГГГГ-ММ-ДД")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val remainingAmount = when (val parsed = state.remainingAmount.parseOptionalNonNegativeDouble("Введите корректный остаток")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val lowStockThreshold = when (val parsed = state.lowStockThreshold.parseOptionalNonNegativeDouble("Введите корректный порог низкого запаса")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val packageAmount = when (val parsed = state.packageAmount.parseOptionalNonNegativeDouble("Введите корректный объем упаковки")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val calories = when (val parsed = state.calories.parseOptionalNonNegativeDouble("Введите корректные калории")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val protein = when (val parsed = state.protein.parseOptionalNonNegativeDouble("Введите корректный белок")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val fat = when (val parsed = state.fat.parseOptionalNonNegativeDouble("Введите корректные жиры")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }
        val carbs = when (val parsed = state.carbs.parseOptionalNonNegativeDouble("Введите корректные углеводы")) {
            ParseResult.Invalid -> return
            is ParseResult.Valid -> parsed.value
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching {
                val editingProductId = productId
                val requestImageUrl = state.imageUrl.takeUnless { state.isImageChanged || state.isImageRemoved }
                val uploadLocalImagePath = state.localImagePath.takeIf {
                    editingProductId == null || state.isImageChanged
                }
                if (editingProductId == null) {
                    addProductUseCase(
                        householdId = householdId,
                        name = state.name,
                        category = state.category,
                        categoryId = state.categoryId,
                        quantity = qty,
                        quantityUnit = state.quantityUnit,
                        expirationDate = expDate,
                        brand = state.brand.trim().ifBlank { null },
                        barcode = state.barcode.trim().ifBlank { null },
                        packageAmount = packageAmount,
                        packageUnit = state.packageUnit.takeIf { packageAmount != null },
                        ingredientsText = state.ingredientsText.trim().ifBlank { null },
                        imageUrl = requestImageUrl,
                        localImagePath = uploadLocalImagePath,
                        calories = calories,
                        protein = protein,
                        fat = fat,
                        carbs = carbs,
                        remainingAmount = remainingAmount,
                        lowStockThreshold = lowStockThreshold
                    )
                } else {
                    updateProductUseCase(
                        householdId = householdId,
                        productId = editingProductId,
                        name = state.name,
                        category = state.category,
                        categoryId = state.categoryId,
                        quantity = qty,
                        quantityUnit = state.quantityUnit,
                        expirationDate = expDate,
                        brand = state.brand.trim().ifBlank { null },
                        barcode = state.barcode.trim().ifBlank { null },
                        packageAmount = packageAmount,
                        packageUnit = state.packageUnit.takeIf { packageAmount != null },
                        ingredientsText = state.ingredientsText.trim().ifBlank { null },
                        imageUrl = requestImageUrl,
                        localImagePath = uploadLocalImagePath,
                        clearImage = state.isImageRemoved,
                        calories = calories,
                        protein = protein,
                        fat = fat,
                        carbs = carbs,
                        remainingAmount = remainingAmount,
                        lowStockThreshold = lowStockThreshold
                    )
                }
            }.onSuccess {
                updateState { copy(isLoading = false) }
                sendAction(AddProductAction.ProductAdded)
            }.onFailure { error ->
                updateState { copy(isLoading = false, error = error.message) }
                sendAction(AddProductAction.ShowError(error.message ?: "Ошибка"))
            }
        }
    }

    private fun String.parseOptionalDate(message: String): ParseResult<LocalDate?> {
        if (isBlank()) return ParseResult.Valid(null)
        return runCatching<ParseResult<LocalDate?>> { ParseResult.Valid(LocalDate.parse(this)) }
            .getOrElse {
                updateState { copy(error = message) }
                sendAction(AddProductAction.ShowError(message))
                ParseResult.Invalid
            }
    }

    private fun String.parseOptionalNonNegativeDouble(message: String): ParseResult<Double?> {
        if (isBlank()) return ParseResult.Valid(null)
        val value = toDoubleOrNull()
        if (value != null && value >= 0.0) return ParseResult.Valid(value)
        updateState { copy(error = message) }
        sendAction(AddProductAction.ShowError(message))
        return ParseResult.Invalid
    }

    private sealed interface ParseResult<out T> {
        data class Valid<T>(val value: T) : ParseResult<T>
        data object Invalid : ParseResult<Nothing>
    }

    private fun List<ProductCategoryOption>.idFor(category: ProductCategory): String? =
        firstOrNull { it.code == category }?.id

    private fun ProductEnrichmentSuggestion.message(): String =
        when (source) {
            ProductEnrichmentSource.GIGACHAT -> "Подсказка GigaChat: $categoryName"
            ProductEnrichmentSource.RULE_BASED -> "Подсказка по правилам: $categoryName"
            ProductEnrichmentSource.FALLBACK -> "Подсказка по умолчанию: $categoryName"
        }

    private fun Double.formatNumber(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun AddProductState.fromProduct(product: Product): AddProductState =
        copy(
            name = product.name,
            brand = product.brand.orEmpty(),
            barcode = product.barcode.orEmpty(),
            category = product.category,
            categoryId = product.categoryId ?: categories.idFor(product.category),
            quantity = product.quantity.formatNumber(),
            quantityUnit = product.quantityUnit,
            remainingAmount = product.remainingAmount.formatNumber(),
            lowStockThreshold = product.lowStockThreshold?.formatNumber().orEmpty(),
            expirationDate = product.expirationDate?.toString().orEmpty(),
            packageAmount = product.packageAmount?.formatNumber().orEmpty(),
            packageUnit = product.packageUnit ?: product.quantityUnit,
            ingredientsText = product.ingredientsText.orEmpty(),
            imageUrl = product.imageUrl,
            localImagePath = product.localImagePath,
            isImageRemoved = false,
            isImageChanged = false,
            calories = product.calories?.formatNumber().orEmpty(),
            protein = product.protein?.formatNumber().orEmpty(),
            fat = product.fat?.formatNumber().orEmpty(),
            carbs = product.carbs?.formatNumber().orEmpty(),
            isBarcodePrefilled = product.barcode != null,
            error = null
        )
}
