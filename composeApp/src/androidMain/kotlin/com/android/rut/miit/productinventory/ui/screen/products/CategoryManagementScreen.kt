package com.android.rut.miit.productinventory.ui.screen.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.R
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.presentation.categories.CategoryManagementEvent
import com.android.rut.miit.productinventory.feature.products.presentation.categories.CategoryManagementViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    householdId: String,
    onBack: () -> Unit,
    viewModel: CategoryManagementViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(householdId) {
        viewModel.onEvent(CategoryManagementEvent.OnCreate(householdId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NewCategoryCard(
                        name = state.newCategoryName,
                        isSaving = state.isSaving,
                        onNameChanged = { viewModel.onEvent(CategoryManagementEvent.OnNewCategoryNameChanged(it)) },
                        onCreate = { viewModel.onEvent(CategoryManagementEvent.OnCreateCategoryClick) }
                    )
                }

                state.error?.let { error ->
                    item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                }

                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        name = state.editableNames[category.id] ?: category.name,
                        isSaving = state.isSaving,
                        onNameChanged = {
                            viewModel.onEvent(CategoryManagementEvent.OnCategoryNameChanged(category.id, it))
                        },
                        onUpdate = { viewModel.onEvent(CategoryManagementEvent.OnUpdateCategoryClick(category.id)) },
                        onArchive = { viewModel.onEvent(CategoryManagementEvent.OnArchiveCategoryClick(category.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NewCategoryCard(
    name: String,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onCreate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.category_new_title),
                style = MaterialTheme.typography.titleSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.product_new_category_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onCreate, enabled = !isSaving) {
                    Text(stringResource(R.string.product_create_category))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: ProductCategoryOption,
    name: String,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onUpdate: () -> Unit,
    onArchive: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (category.system) {
                    stringResource(R.string.category_system_badge)
                } else {
                    stringResource(R.string.category_custom_badge)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (category.system) {
                Text(
                    text = categoryDisplayName(category),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.product_category_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onUpdate, enabled = !isSaving) {
                        Text(stringResource(R.string.save))
                    }
                    TextButton(onClick = onArchive, enabled = !isSaving) {
                        Text(
                            text = stringResource(R.string.category_archive),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun categoryDisplayName(category: ProductCategoryOption): String =
    category.code?.let { code ->
        when (code) {
            ProductCategory.DAIRY -> stringResource(R.string.category_dairy)
            ProductCategory.MEAT_FISH -> stringResource(R.string.category_meat_fish)
            ProductCategory.VEGETABLES_FRUITS -> stringResource(R.string.category_vegetables_fruits)
            ProductCategory.CEREALS -> stringResource(R.string.category_cereals)
            ProductCategory.BEVERAGES -> stringResource(R.string.category_beverages)
            ProductCategory.OTHER -> stringResource(R.string.category_other)
        }
    } ?: category.name
