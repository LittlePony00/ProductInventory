package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
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
                title = { Text("Продукты") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToRecipes) { Text("Рецепты") }
                    TextButton(onClick = onNavigateToNotifications) { Text("🔔") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ProductListEvent.OnAddProductClick) }
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
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
                                text = "Нет продуктов",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нажмите + чтобы добавить",
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
                            text = currentState.message ?: "Произошла ошибка",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.onEvent(ProductListEvent.OnRetry) }) {
                            Text("Повторить")
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
                    text = "${product.quantity} ${unitDisplayName(product.quantityUnit)}",
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
                        text = "Годен до: $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }

            TextButton(onClick = onDelete) {
                Text(
                    "✕",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun unitDisplayName(unit: com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit): String = when (unit) {
    com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit.GRAMS -> "г"
    com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit.MILLILITERS -> "мл"
    com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit.PIECES -> "шт"
}
