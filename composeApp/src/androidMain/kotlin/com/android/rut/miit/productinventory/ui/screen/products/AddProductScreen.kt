package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.presentation.add.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    householdId: String,
    onProductAdded: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddProductViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(householdId) { viewModel.householdId = householdId }

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
                title = { Text("Добавить продукт") },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(AddProductEvent.OnBackClick) }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnNameChanged(it)) },
                label = { Text("Название продукта") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = categoryDisplayName(state.category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    ProductCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(categoryDisplayName(cat)) },
                            onClick = {
                                viewModel.onEvent(AddProductEvent.OnCategoryChanged(cat))
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onEvent(AddProductEvent.OnQuantityChanged(it)) },
                    label = { Text("Количество") },
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
                        label = { Text("Ед. изм.") },
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

            OutlinedTextField(
                value = state.expirationDate,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnExpirationDateChanged(it)) },
                label = { Text("Срок годности (ГГГГ-ММ-ДД)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.onEvent(AddProductEvent.OnSaveClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Сохранить")
                }
            }
        }
    }
}

private fun categoryDisplayName(category: ProductCategory): String = when (category) {
    ProductCategory.DAIRY -> "Молочные"
    ProductCategory.MEAT_FISH -> "Мясо/Рыба"
    ProductCategory.VEGETABLES_FRUITS -> "Овощи/Фрукты"
    ProductCategory.CEREALS -> "Крупы"
    ProductCategory.BEVERAGES -> "Напитки"
    ProductCategory.OTHER -> "Другое"
}

private fun unitDisplayName(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.GRAMS -> "Граммы"
    QuantityUnit.MILLILITERS -> "Миллилитры"
    QuantityUnit.PIECES -> "Штуки"
}
