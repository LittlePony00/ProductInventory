package com.android.rut.miit.productinventory.ui.screen.products

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.android.rut.miit.productinventory.core.R
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.presentation.add.*
import com.android.rut.miit.productinventory.ui.design.SectionCard
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    householdId: String,
    productId: String? = null,
    barcode: String? = null,
    initialName: String? = null,
    initialBrand: String? = null,
    initialCategory: String? = null,
    initialPackageAmount: String? = null,
    initialPackageUnit: String? = null,
    initialIngredientsText: String? = null,
    initialImageUrl: String? = null,
    initialLocalImagePath: String? = null,
    initialCalories: String? = null,
    initialProtein: String? = null,
    initialFat: String? = null,
    initialCarbs: String? = null,
    onProductAdded: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddProductViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var packageUnitExpanded by remember { mutableStateOf(false) }
    var isProcessingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isProcessingPhoto = true
            photoError = null
            val localPath = withContext(Dispatchers.IO) {
                uri.copyProductImageToLocalFile(context)
            }
            if (localPath != null) {
                viewModel.onEvent(AddProductEvent.OnImageSelected(localPath))
            } else {
                photoError = context.getString(R.string.product_photo_prepare_error)
            }
            isProcessingPhoto = false
        }
    }

    LaunchedEffect(
        householdId,
        productId,
        barcode,
        initialName,
        initialBrand,
        initialCategory,
        initialPackageAmount,
        initialPackageUnit,
        initialIngredientsText,
        initialImageUrl,
        initialLocalImagePath,
        initialCalories,
        initialProtein,
        initialFat,
        initialCarbs
    ) {
        viewModel.onEvent(AddProductEvent.OnCreate(householdId))
        productId?.let { viewModel.onEvent(AddProductEvent.OnLoadProduct(it)) }
        viewModel.onEvent(
            AddProductEvent.OnPrefill(
                barcode = barcode,
                name = initialName,
                brand = initialBrand,
                category = initialCategory?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
                packageAmount = initialPackageAmount,
                packageUnit = initialPackageUnit?.let { runCatching { QuantityUnit.valueOf(it) }.getOrNull() },
                ingredientsText = initialIngredientsText,
                imageUrl = initialImageUrl,
                localImagePath = initialLocalImagePath,
                calories = initialCalories,
                protein = initialProtein,
                fat = initialFat,
                carbs = initialCarbs
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is AddProductAction.ProductAdded -> onProductAdded()
                is AddProductAction.NavigateBack -> onBack()
                is AddProductAction.ShowError -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (productId == null) R.string.product_add_title else R.string.product_edit_title
                        )
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(AddProductEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.onEvent(AddProductEvent.OnSaveClick) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(
                title = stringResource(R.string.product_main_section),
                subtitle = stringResource(R.string.product_main_section_hint)
            ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnNameChanged(it)) },
                label = { Text(stringResource(R.string.product_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.brand,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnBrandChanged(it)) },
                label = { Text(stringResource(R.string.product_brand_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.barcode,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnBarcodeChanged(it)) },
                label = { Text(stringResource(R.string.product_barcode_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = if (state.isBarcodePrefilled) {
                    { Text(stringResource(R.string.product_barcode_prefilled_hint)) }
                } else {
                    null
                }
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.categories
                        .firstOrNull { it.id == state.categoryId }
                        ?.let { categoryOptionDisplayName(it) }
                        ?: categoryDisplayName(state.category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product_category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(categoryOptionDisplayName(category)) },
                            onClick = {
                                viewModel.onEvent(
                                    AddProductEvent.OnCategoryChanged(
                                        categoryId = category.id,
                                        category = category.legacyCategory
                                    )
                                )
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.newCategoryName,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnNewCategoryNameChanged(it)) },
                    label = { Text(stringResource(R.string.product_new_category_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.onEvent(AddProductEvent.OnCreateCategoryClick) },
                    enabled = !state.isCreatingCategory,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (state.isCreatingCategory) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.product_create_category))
                    }
                }
            }
            }

            SectionCard(
                title = stringResource(R.string.product_photo_section),
                subtitle = stringResource(R.string.product_photo_section_hint)
            ) {
                ProductPhotoEditor(
                    imageUrl = state.imageUrl,
                    localImagePath = state.localImagePath,
                    isProcessingPhoto = isProcessingPhoto,
                    photoError = photoError,
                    onPick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = { viewModel.onEvent(AddProductEvent.OnImageRemoved) }
                )
            }

            SectionCard(
                title = stringResource(R.string.product_inventory_section),
                subtitle = stringResource(R.string.product_inventory_section_hint)
            ) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnQuantityChanged(it)) },
                    label = { Text(stringResource(R.string.product_quantity_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unitDisplayName(state.quantityUnit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.product_unit_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unitDisplayName(unit)) },
                                onClick = {
                                    viewModel.onEvent(AddProductEvent.OnQuantityUnitChanged(unit))
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.remainingAmount,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnRemainingAmountChanged(it)) },
                    label = { Text(stringResource(R.string.product_remaining_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = state.lowStockThreshold,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnLowStockThresholdChanged(it)) },
                    label = { Text(stringResource(R.string.product_low_stock_threshold_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            }

            SectionCard(
                title = stringResource(R.string.product_expiration_section),
                subtitle = stringResource(R.string.product_expiration_section_hint)
            ) {
            OutlinedTextField(
                value = state.expirationDate,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnExpirationDateChanged(it)) },
                label = { Text(stringResource(R.string.product_expiration_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            }

            SectionCard(
                title = stringResource(R.string.product_ai_suggestions_section),
                subtitle = stringResource(R.string.product_ai_suggestions_section_hint)
            ) {

            Button(
                onClick = { viewModel.onEvent(AddProductEvent.OnSuggestProductClick) },
                enabled = !state.isSuggestingProduct && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSuggestingProduct) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.product_ai_suggest_button))
                }
            }

            state.suggestionMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.packageAmount,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnPackageAmountChanged(it)) },
                    label = { Text(stringResource(R.string.product_package_amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                ExposedDropdownMenuBox(
                    expanded = packageUnitExpanded,
                    onExpandedChange = { packageUnitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unitDisplayName(state.packageUnit),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.product_package_unit_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(packageUnitExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = packageUnitExpanded,
                        onDismissRequest = { packageUnitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unitDisplayName(unit)) },
                                onClick = {
                                    viewModel.onEvent(AddProductEvent.OnPackageUnitChanged(unit))
                                    packageUnitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.ingredientsText,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnIngredientsChanged(it)) },
                label = { Text(stringResource(R.string.product_ingredients_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.calories,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnCaloriesChanged(it)) },
                    label = { Text(stringResource(R.string.product_calories_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.protein,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnProteinChanged(it)) },
                    label = { Text(stringResource(R.string.product_protein_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.fat,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnFatChanged(it)) },
                    label = { Text(stringResource(R.string.product_fat_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.carbs,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnCarbsChanged(it)) },
                    label = { Text(stringResource(R.string.product_carbs_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun categoryOptionDisplayName(category: ProductCategoryOption): String =
    category.code?.let { categoryDisplayName(it) } ?: category.name

@Composable
private fun categoryDisplayName(category: ProductCategory): String = when (category) {
    ProductCategory.DAIRY -> stringResource(R.string.category_dairy)
    ProductCategory.MEAT_FISH -> stringResource(R.string.category_meat_fish)
    ProductCategory.VEGETABLES_FRUITS -> stringResource(R.string.category_vegetables_fruits)
    ProductCategory.CEREALS -> stringResource(R.string.category_cereals)
    ProductCategory.BEVERAGES -> stringResource(R.string.category_beverages)
    ProductCategory.OTHER -> stringResource(R.string.category_other)
}

@Composable
private fun unitDisplayName(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.GRAMS -> stringResource(R.string.unit_grams)
    QuantityUnit.MILLILITERS -> stringResource(R.string.unit_milliliters)
    QuantityUnit.PIECES -> stringResource(R.string.unit_pieces)
}

@Composable
private fun ProductPhotoEditor(
    imageUrl: String?,
    localImagePath: String?,
    isProcessingPhoto: Boolean,
    photoError: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    var useRemoteFallback by remember(localImagePath, imageUrl) { mutableStateOf(false) }
    val localImageFile = localImagePath?.let(::File)?.takeIf { it.exists() }
    val imageModel = if (!useRemoteFallback && localImageFile != null) localImageFile else imageUrl
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = stringResource(R.string.product_photo_placeholder),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        if (localImageFile != null && imageUrl != null) {
                            useRemoteFallback = true
                        }
                    }
                )
            } else {
                Text(
                    text = stringResource(R.string.product_photo_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPick, enabled = !isProcessingPhoto) {
                Text(
                    stringResource(
                        if (imageModel == null) R.string.product_photo_add else R.string.product_photo_replace
                    )
                )
            }
            if (imageModel != null) {
                TextButton(onClick = onRemove, enabled = !isProcessingPhoto) {
                    Text(stringResource(R.string.product_photo_remove))
                }
            }
        }
        if (isProcessingPhoto) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.product_photo_preparing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        photoError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun Uri.copyProductImageToLocalFile(context: Context): String? =
    runCatching {
        val directory = File(context.filesDir, "product-images").apply { mkdirs() }
        val target = File(directory, "${UUID.randomUUID()}.jpg")
        val decoded = context.contentResolver.openInputStream(this)?.use { input ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, bounds)
            bounds
        }?.let { bounds ->
            context.contentResolver.openInputStream(this)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                    inSampleSize = bounds.inSampleSize(maxDimension = 800)
                })
            }
        } ?: error("Cannot decode image")
        val bitmap = decoded.resized(maxDimension = 800)
        target.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 35, output)
        }
        if (bitmap != decoded) decoded.recycle()
        bitmap.recycle()
        target.absolutePath
    }.getOrNull()

private fun BitmapFactory.Options.inSampleSize(maxDimension: Int): Int {
    var sampleSize = 1
    while ((outWidth / sampleSize) > maxDimension || (outHeight / sampleSize) > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

private fun Bitmap.resized(maxDimension: Int): Bitmap {
    val maxCurrentDimension = maxOf(width, height)
    if (maxCurrentDimension <= maxDimension) return this
    val scale = maxDimension.toFloat() / maxCurrentDimension
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        true
    )
}
