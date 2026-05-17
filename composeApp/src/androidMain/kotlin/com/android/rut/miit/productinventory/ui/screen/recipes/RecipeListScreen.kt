package com.android.rut.miit.productinventory.ui.screen.recipes

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
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.presentation.*
import com.android.rut.miit.productinventory.ui.design.ScreenError
import com.android.rut.miit.productinventory.ui.design.ScreenLoading
import com.android.rut.miit.productinventory.ui.design.ScreenMessage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    householdId: String,
    onBack: () -> Unit,
    viewModel: RecipeListViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(householdId) {
        viewModel.onEvent(RecipeListEvent.OnCreate(householdId))
    }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is RecipeListAction.OpenRecipeDetail -> {}
                is RecipeListAction.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recipes_title)) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(RecipeListEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(RecipeListEvent.OnGenerateClick) },
                        enabled = state !is RecipeListState.Loading
                    ) {
                        Text(stringResource(R.string.recipes_generate))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is RecipeListState.Loading -> {
                ScreenLoading(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is RecipeListState.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.recipes) { recipe ->
                        RecipeCard(recipe)
                    }
                }
            }
            is RecipeListState.Empty -> {
                ScreenMessage(
                    title = if (s.generated) {
                        stringResource(R.string.recipes_no_suggestions)
                    } else {
                        stringResource(R.string.recipes_empty)
                    },
                    message = if (s.generated) {
                        stringResource(R.string.recipes_no_suggestions_hint)
                    } else {
                        stringResource(R.string.recipes_empty_hint)
                    },
                    iconText = "*",
                    primaryActionLabel = if (s.generated) null else stringResource(R.string.recipes_generate),
                    onPrimaryAction = if (s.generated) null else {
                        { viewModel.onEvent(RecipeListEvent.OnGenerateClick) }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            is RecipeListState.Error -> {
                ScreenError(
                    message = s.message ?: stringResource(R.string.error_loading),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.onEvent(RecipeListEvent.OnRetry) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("${recipe.time} • ${recipe.calories} kcal", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.recipes_ingredients), style = MaterialTheme.typography.labelMedium)
            recipe.ingredients.forEach { ingredient ->
                Text("- ${ingredient.name}: ${ingredient.amount}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.recipes_instructions), style = MaterialTheme.typography.labelMedium)
            recipe.steps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
