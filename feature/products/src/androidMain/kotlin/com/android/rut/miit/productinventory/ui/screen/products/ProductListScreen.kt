package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import coil3.compose.AsyncImage
import com.android.rut.miit.productinventory.core.R
import com.android.rut.miit.productinventory.core.push.scheduleProductLocalReminders
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.api.models.customCategoryNameForDisplay
import com.android.rut.miit.productinventory.feature.products.presentation.list.*
import com.android.rut.miit.productinventory.ui.design.DestructiveTextButton
import com.android.rut.miit.productinventory.ui.design.ScreenError
import com.android.rut.miit.productinventory.ui.design.ScreenLoading
import com.android.rut.miit.productinventory.ui.design.ScreenMessage
import com.android.rut.miit.productinventory.ui.design.StatusPill
import com.android.rut.miit.productinventory.ui.design.UiTone
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    householdId: String,
    onAddProduct: () -> Unit = {},
    onEditProduct: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    onNavigateToRecipes: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToBarcodeScan: () -> Unit = {},
    viewModel: ProductListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var consumingProduct by remember { mutableStateOf<Product?>(null) }
    var consumeAmount by remember { mutableStateOf("") }
    var consumeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(householdId) {
        viewModel.onEvent(ProductListEvent.OnCreate(householdId))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(ProductListEvent.OnResume)
    }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is ProductListAction.OpenAddProduct -> onAddProduct()
                is ProductListAction.OpenProductDetail -> onEditProduct(action.productId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.products_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToBarcodeScan,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(stringResource(R.string.scan), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(ProductListEvent.OnAddProductClick) }
                ) {
                    Text("+  ${stringResource(R.string.add)}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    ) { padding ->
        when (val currentState = state) {
            is ProductListState.Loading -> {
                ScreenLoading(modifier = Modifier.fillMaxSize().padding(padding))
            }

            is ProductListState.Content -> {
                LaunchedEffect(currentState.localReminders) {
                    context.scheduleProductLocalReminders(currentState.localReminders)
                }

                if (currentState.products.isEmpty()) {
                    ScreenMessage(
                        title = stringResource(R.string.products_empty),
                        message = stringResource(R.string.products_empty_hint),
                        iconText = "+",
                        primaryActionLabel = stringResource(R.string.add),
                        onPrimaryAction = { viewModel.onEvent(ProductListEvent.OnAddProductClick) },
                        secondaryActionLabel = stringResource(R.string.scan),
                        onSecondaryAction = onNavigateToBarcodeScan,
                        modifier = Modifier.fillMaxSize().padding(padding),
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        ProductQuickActions(
                            onManageCategories = onManageCategories,
                            onNavigateToRecipes = onNavigateToRecipes,
                            onNavigateToNotifications = onNavigateToNotifications,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ProductFilters(
                            filters = currentState.filters,
                            categories = currentState.categories,
                            isRealtimeActive = currentState.isRealtimeActive,
                            onCategorySelected = {
                                viewModel.onEvent(ProductListEvent.OnCategoryFilterChanged(it))
                            },
                            onInventoryFilterSelected = {
                                viewModel.onEvent(ProductListEvent.OnInventoryFilterChanged(it))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (currentState.visibleProducts.isEmpty()) {
                            ScreenMessage(
                                title = stringResource(R.string.products_no_filter_results),
                                message = stringResource(R.string.products_no_filter_results_hint),
                                iconText = "0",
                                primaryActionLabel = stringResource(R.string.filter_clear),
                                onPrimaryAction = {
                                    viewModel.onEvent(ProductListEvent.OnCategoryFilterChanged(null))
                                    viewModel.onEvent(ProductListEvent.OnInventoryFilterChanged(InventoryFilter.ALL))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentState.visibleProducts, key = { it.id }) { product ->
                                    ProductCard(
                                        product = product,
                                        onClick = { viewModel.onEvent(ProductListEvent.OnProductClick(product.id)) },
                                        onConsume = {
                                            consumingProduct = product
                                            consumeAmount = product.remainingAmount.toString()
                                            consumeError = null
                                        },
                                        onDelete = { viewModel.onEvent(ProductListEvent.OnDeleteProduct(product.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is ProductListState.Error -> {
                ScreenError(
                    message = currentState.message ?: stringResource(R.string.error_generic),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.onEvent(ProductListEvent.OnRetry) },
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
    }

    consumingProduct?.let { product ->
        val invalidConsumeMessage = stringResource(R.string.product_consume_error)
        AlertDialog(
            onDismissRequest = {
                consumingProduct = null
                consumeError = null
            },
            title = { Text(stringResource(R.string.product_consume_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name)
                    Text(
                        text = stringResource(
                            R.string.product_consume_remaining,
                            product.remainingAmount,
                            unitShortName(product.quantityUnit)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = consumeAmount,
                        onValueChange = {
                            consumeAmount = it
                            consumeError = null
                        },
                        label = { Text(stringResource(R.string.product_consume_amount_label)) },
                        isError = consumeError != null,
                        singleLine = true
                    )
                    consumeError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = consumeAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0.0 || amount > product.remainingAmount) {
                            consumeError = invalidConsumeMessage
                            return@TextButton
                        }
                        viewModel.onEvent(ProductListEvent.OnConsumeProduct(product.id, amount))
                        consumingProduct = null
                        consumeError = null
                    }
                ) {
                    Text(stringResource(R.string.product_consume))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        consumingProduct = null
                        consumeError = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProductQuickActions(
    onManageCategories: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AssistChip(
                onClick = onManageCategories,
                label = { Text(stringResource(R.string.categories_title), maxLines = 1) }
            )
        }
        item {
            AssistChip(
                onClick = onNavigateToRecipes,
                label = { Text(stringResource(R.string.recipes_title), maxLines = 1) }
            )
        }
        item {
            AssistChip(
                onClick = onNavigateToNotifications,
                label = { Text(stringResource(R.string.notifications_title), maxLines = 1) }
            )
        }
    }
}

@Composable
private fun ProductFilters(
    filters: ProductListFilters,
    categories: List<ProductCategoryOption>,
    isRealtimeActive: Boolean,
    onCategorySelected: (String?) -> Unit,
    onInventoryFilterSelected: (InventoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = filters.categoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text(stringResource(R.string.filter_all), maxLines = 1) }
                )
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = filters.categoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(categoryOptionDisplayName(category), maxLines = 1) }
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(InventoryFilter.entries, key = { it.name }) { filter ->
                FilterChip(
                    selected = filters.inventory == filter,
                    onClick = { onInventoryFilterSelected(filter) },
                    label = { Text(inventoryFilterName(filter), maxLines = 1) }
                )
            }
            if (isRealtimeActive) {
                item {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.products_realtime_active), maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onConsume: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val categoryName = productCategoryDisplayName(product)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${product.name}, $categoryName"
            },
        colors = CardDefaults.cardColors(
            containerColor = when (product.expirationStatus) {
                ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.36f)
                ExpirationStatus.EXPIRING_SOON -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                ProductThumbnail(
                    product = product,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    product.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    text = expirationStatusLabel(product.expirationStatus),
                    tone = expirationStatusTone(product.expirationStatus)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val progress = if (product.quantity <= 0.0) {
                    0f
                } else {
                    (product.remainingAmount / product.quantity).toFloat().coerceIn(0f, 1f)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (product.isLowStock) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = stringResource(
                        R.string.products_inventory_level,
                        product.remainingAmount,
                        product.quantity,
                        unitShortName(product.quantityUnit)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.isLowStock) {
                    StatusPill(
                        text = stringResource(R.string.products_low_stock_badge),
                        tone = UiTone.Error
                    )
                }
                product.expirationDate?.let { date ->
                    Text(
                        text = stringResource(R.string.products_expiration, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onConsume,
                    enabled = product.remainingAmount > 0.0
                ) {
                    Text(stringResource(R.string.product_consume), maxLines = 1)
                }
                DestructiveTextButton(
                    text = stringResource(R.string.delete),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.product_delete_confirm_title)) },
            text = { Text(stringResource(R.string.product_delete_confirm_message, product.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProductThumbnail(product: Product, modifier: Modifier = Modifier) {
    var useRemoteFallback by remember(product.localImagePath, product.imageUrl) { mutableStateOf(false) }
    val localImageFile = product.localImagePath?.let(::File)?.takeIf { it.exists() }
    val imageModel = if (!useRemoteFallback && localImageFile != null) localImageFile else product.imageUrl
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (localImageFile != null && product.imageUrl != null) {
                        useRemoteFallback = true
                    }
                }
            )
        } else {
            Text(
                text = stringResource(R.string.product_photo_placeholder),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun categoryOptionDisplayName(category: ProductCategoryOption): String =
    category.code?.let { categoryDisplayName(it) } ?: category.name

@Composable
private fun productCategoryDisplayName(product: Product): String =
    if (product.customCategoryNameForDisplay() != null) {
        product.categoryName.orEmpty()
    } else {
        categoryDisplayName(product.category)
    }

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
private fun inventoryFilterName(filter: InventoryFilter): String = when (filter) {
    InventoryFilter.ALL -> stringResource(R.string.filter_all)
    InventoryFilter.LOW_STOCK -> stringResource(R.string.filter_low_stock)
    InventoryFilter.EXPIRING_SOON -> stringResource(R.string.filter_expiring_soon)
    InventoryFilter.EXPIRED -> stringResource(R.string.filter_expired)
}

@Composable
private fun expirationStatusLabel(status: ExpirationStatus): String = when (status) {
    ExpirationStatus.FRESH -> stringResource(R.string.expiration_fresh)
    ExpirationStatus.EXPIRING_SOON -> stringResource(R.string.expiration_soon)
    ExpirationStatus.EXPIRED -> stringResource(R.string.expiration_expired)
    ExpirationStatus.UNKNOWN -> stringResource(R.string.expiration_unknown)
}

private fun expirationStatusTone(status: ExpirationStatus): UiTone = when (status) {
    ExpirationStatus.FRESH -> UiTone.Success
    ExpirationStatus.EXPIRING_SOON -> UiTone.Warning
    ExpirationStatus.EXPIRED -> UiTone.Error
    ExpirationStatus.UNKNOWN -> UiTone.Neutral
}

@Composable
private fun unitShortName(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.GRAMS -> stringResource(R.string.unit_grams_short)
    QuantityUnit.MILLILITERS -> stringResource(R.string.unit_milliliters_short)
    QuantityUnit.PIECES -> stringResource(R.string.unit_pieces_short)
}
