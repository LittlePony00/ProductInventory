package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
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
                title = { Text(stringResource(R.string.product_add_title)) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(AddProductEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
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
                label = { Text(stringResource(R.string.product_name_label)) },
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
                    label = { Text(stringResource(R.string.product_category_label)) },
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
                    label = { Text(stringResource(R.string.product_quantity_label)) },
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

            OutlinedTextField(
                value = state.expirationDate,
                onValueChange = { viewModel.onEvent(AddProductEvent.OnExpirationDateChanged(it)) },
                label = { Text(stringResource(R.string.product_expiration_label)) },
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
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
private fun unitDisplayName(unit: QuantityUnit): String = when (unit) {
    QuantityUnit.GRAMS -> stringResource(R.string.unit_grams)
    QuantityUnit.MILLILITERS -> stringResource(R.string.unit_milliliters)
    QuantityUnit.PIECES -> stringResource(R.string.unit_pieces)
}
