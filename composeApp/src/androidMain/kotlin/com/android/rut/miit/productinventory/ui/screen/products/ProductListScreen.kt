package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.presentation.list.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    householdId: String,
    onAddProduct: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateToRecipes: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToBarcodeScan: () -> Unit = {},
    viewModel: ProductListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(householdId) {
        viewModel.onEvent(ProductListEvent.OnCreate(householdId))
    }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is ProductListAction.OpenAddProduct -> onAddProduct()
                is ProductListAction.OpenProductDetail -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToRecipes) { Text(stringResource(R.string.recipes_title)) }
                    TextButton(onClick = onNavigateToNotifications) { Text(stringResource(R.string.notifications_title)) }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onNavigateToBarcodeScan,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(stringResource(R.string.scan), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { viewModel.onEvent(ProductListEvent.OnAddProductClick) }
                ) {
                    Text(stringResource(R.string.add), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    ) { padding ->
        when (val currentState = state) {
            is ProductListState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProductListState.Content -> {
                if (currentState.products.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.products_empty),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.products_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentState.products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { viewModel.onEvent(ProductListEvent.OnProductClick(product.id)) },
                                onDelete = { viewModel.onEvent(ProductListEvent.OnDeleteProduct(product.id)) }
                            )
                        }
                    }
                }
            }

            is ProductListState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentState.message ?: stringResource(R.string.error_generic),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onEvent(ProductListEvent.OnRetry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${product.quantity} ${unitShortName(product.quantityUnit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                product.expirationDate?.let { date ->
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusColor = when (product.expirationStatus) {
                        ExpirationStatus.EXPIRED -> MaterialTheme.colorScheme.error
                        ExpirationStatus.EXPIRING_SOON -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = stringResource(R.string.products_expiration, date),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }

            TextButton(onClick = onDelete) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun unitShortName(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.GRAMS -> stringResource(R.string.unit_grams_short)
    QuantityUnit.MILLILITERS -> stringResource(R.string.unit_milliliters_short)
    QuantityUnit.PIECES -> stringResource(R.string.unit_pieces_short)
}
