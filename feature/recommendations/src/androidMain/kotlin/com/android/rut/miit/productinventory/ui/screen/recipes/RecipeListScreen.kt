package com.android.rut.miit.productinventory.ui.screen.recipes

import androidx.compose.foundation.clickable
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
import com.android.rut.miit.productinventory.core.R
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeQuickFilter
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
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
                        onClick = { viewModel.onEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick) },
                        enabled = state !is RecipeListState.Loading
                    ) {
                        Text(stringResource(R.string.recipes_generate_current))
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
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    RecipeTabs(
                        selectedTab = s.selectedTab,
                        onTabSelected = { viewModel.onEvent(RecipeListEvent.OnTabSelected(it)) }
                    )
                    if (s.selectedTab == RecipeListTab.DISCOVER) {
                        RecipeActions(
                            selectedMode = s.selectedMode,
                            quickFilters = s.quickFilters,
                            onGenerateCurrent = { viewModel.onEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick) },
                            onUseSoon = { viewModel.onEvent(RecipeListEvent.OnUseSoonClick) },
                            onQuickFilterClick = { viewModel.onEvent(RecipeListEvent.OnQuickFilterChanged(it)) }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.recipes) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                liked = recipe.localIdentity() in s.likedRecipeIds,
                                onLikeClick = { viewModel.onEvent(RecipeListEvent.OnRecipeLikeClick(recipe)) }
                            )
                        }
                    }
                }
            }
            is RecipeListState.Empty -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    RecipeTabs(
                        selectedTab = s.selectedTab,
                        onTabSelected = { viewModel.onEvent(RecipeListEvent.OnTabSelected(it)) }
                    )
                    if (s.selectedTab == RecipeListTab.DISCOVER) {
                        RecipeActions(
                            selectedMode = s.mode,
                            quickFilters = s.quickFilters,
                            onGenerateCurrent = { viewModel.onEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick) },
                            onUseSoon = { viewModel.onEvent(RecipeListEvent.OnUseSoonClick) },
                            onQuickFilterClick = { viewModel.onEvent(RecipeListEvent.OnQuickFilterChanged(it)) }
                        )
                    }
                    ScreenMessage(
                        title = if (s.selectedTab == RecipeListTab.LIKED) {
                            stringResource(R.string.recipes_no_liked)
                        } else if (s.mode == RecommendationMode.USE_SOON) {
                            stringResource(R.string.recipes_no_suggestions)
                        } else if (s.generated) {
                            stringResource(R.string.recipes_no_suggestions)
                        } else {
                            stringResource(R.string.recipes_empty)
                        },
                        message = if (s.selectedTab == RecipeListTab.LIKED) {
                            stringResource(R.string.recipes_no_liked_hint)
                        } else if (s.generated) {
                            stringResource(R.string.recipes_no_suggestions_hint)
                        } else {
                            stringResource(R.string.recipes_empty_hint)
                        },
                        iconText = "*",
                        primaryActionLabel = stringResource(R.string.recipes_generate_current),
                        onPrimaryAction = { viewModel.onEvent(RecipeListEvent.OnGenerateFromCurrentProductsClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
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

    if (state.showIngredientDialog) {
        RecipeIngredientSelectionDialog(
            options = state.ingredientOptions,
            selectedIds = state.selectedIngredientIds,
            loading = state.ingredientOptionsLoading,
            error = state.ingredientOptionsError,
            onToggle = { viewModel.onEvent(RecipeListEvent.OnIngredientSelectionChanged(it)) },
            onDismiss = { viewModel.onEvent(RecipeListEvent.OnIngredientDialogDismissed) },
            onFindSelected = { viewModel.onEvent(RecipeListEvent.OnFindSelectedRecipeClick) },
            onRandom = { viewModel.onEvent(RecipeListEvent.OnRandomRecipeClick) }
        )
    }
}

@Composable
private fun RecipeTabs(
    selectedTab: RecipeListTab,
    onTabSelected: (RecipeListTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTab == RecipeListTab.DISCOVER,
            onClick = { onTabSelected(RecipeListTab.DISCOVER) },
            label = { Text(stringResource(R.string.recipes_tab_discover)) }
        )
        FilterChip(
            selected = selectedTab == RecipeListTab.LIKED,
            onClick = { onTabSelected(RecipeListTab.LIKED) },
            label = { Text(stringResource(R.string.recipes_tab_liked)) }
        )
    }
}

@Composable
private fun RecipeActions(
    selectedMode: RecommendationMode,
    quickFilters: Set<RecipeQuickFilter>,
    onGenerateCurrent: () -> Unit,
    onUseSoon: () -> Unit,
    onQuickFilterClick: (RecipeQuickFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGenerateCurrent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.recipes_generate_current))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedMode == RecommendationMode.USE_SOON,
                onClick = onUseSoon,
                label = { Text(stringResource(R.string.recipes_use_soon)) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeFilterChip(
                selected = RecipeQuickFilter.UNDER_30_MIN in quickFilters,
                label = stringResource(R.string.recipes_under_30),
                onClick = { onQuickFilterClick(RecipeQuickFilter.UNDER_30_MIN) }
            )
            RecipeFilterChip(
                selected = RecipeQuickFilter.FEW_MISSING_INGREDIENTS in quickFilters,
                label = stringResource(R.string.recipes_few_missing),
                onClick = { onQuickFilterClick(RecipeQuickFilter.FEW_MISSING_INGREDIENTS) }
            )
        }
    }
}

@Composable
private fun RecipeFilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    liked: Boolean,
    onLikeClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(recipe.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onLikeClick) {
                    Text(
                        if (liked) {
                            stringResource(R.string.recipes_unlike_heart)
                        } else {
                            stringResource(R.string.recipes_like_heart)
                        }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${recipe.time} • ${recipe.caloriesLabel()}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            RecipeTags(stringResource(R.string.recipes_expiring_products), recipe.usedExpiringProducts)
            RecipeTags(stringResource(R.string.recipes_used_products), recipe.usedHouseholdProducts)
            RecipeTags(stringResource(R.string.recipes_missing_products), recipe.missingIngredients)
            RecipeTextList(stringResource(R.string.recipes_reasons), recipe.reasons.toUserFacingRecipeNotes())
            RecipeTextList(stringResource(R.string.recipes_warnings), recipe.warnings.toUserFacingRecipeNotes())

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

@Composable
private fun RecipeIngredientSelectionDialog(
    options: List<RecipeIngredientOption>,
    selectedIds: Set<String>,
    loading: Boolean,
    error: String?,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onFindSelected: () -> Unit,
    onRandom: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recipes_select_ingredients_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.recipes_select_ingredients_hint))
                when {
                    loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    options.isEmpty() -> Text(stringResource(R.string.recipes_no_ingredients))
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(options, key = { it.id }) { option ->
                            RecipeIngredientOptionRow(
                                option = option,
                                selected = option.id in selectedIds,
                                onToggle = { onToggle(option.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onFindSelected,
                enabled = selectedIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.recipes_find_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onRandom) {
                Text(stringResource(R.string.recipes_random_recipe))
            }
        }
    )
}

@Composable
private fun RecipeIngredientOptionRow(
    option: RecipeIngredientOption,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(option.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                listOfNotNull(
                    option.categoryName,
                    "${option.remainingAmount} ${option.unit}".takeIf { option.remainingAmount > 0.0 },
                    stringResource(R.string.recipes_use_soon).takeIf { option.expiring }
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecipeTags(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.labelMedium)
    Text(values.joinToString(), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun RecipeTextList(title: String, values: List<String>) {
    if (values.isEmpty()) return
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.labelMedium)
    values.forEach { value ->
        Text("- $value", style = MaterialTheme.typography.bodySmall)
    }
}

private fun Recipe.caloriesLabel(): String =
    if (caloriesKnown) "$calories ккал" else "ккал неизвестно"

private fun List<String>.toUserFacingRecipeNotes(): List<String> =
    mapNotNull(String::toUserFacingRecipeNote).distinct()

private fun String.toUserFacingRecipeNote(): String? =
    replace("AI-Assisted: ", "")
        .replace("Рецепт создан ИИ. ", "")
        .replace("Рецепт создан ИИ, потому что ", "")
        .replace("Рецепт создан ИИ по ", "Рецепт создан по ")
        .trim()
        .takeIf(String::isNotBlank)

private val RecipeListState.showIngredientDialog: Boolean
    get() = when (this) {
        is RecipeListState.Content -> showIngredientDialog
        is RecipeListState.Empty -> showIngredientDialog
        is RecipeListState.Error -> showIngredientDialog
        RecipeListState.Loading -> false
    }

private val RecipeListState.ingredientOptions: List<RecipeIngredientOption>
    get() = when (this) {
        is RecipeListState.Content -> ingredientOptions
        is RecipeListState.Empty -> ingredientOptions
        is RecipeListState.Error -> ingredientOptions
        RecipeListState.Loading -> emptyList()
    }

private val RecipeListState.selectedIngredientIds: Set<String>
    get() = when (this) {
        is RecipeListState.Content -> selectedIngredientIds
        is RecipeListState.Empty -> selectedIngredientIds
        is RecipeListState.Error -> selectedIngredientIds
        RecipeListState.Loading -> emptySet()
    }

private val RecipeListState.ingredientOptionsLoading: Boolean
    get() = when (this) {
        is RecipeListState.Content -> ingredientOptionsLoading
        is RecipeListState.Empty -> ingredientOptionsLoading
        is RecipeListState.Error -> ingredientOptionsLoading
        RecipeListState.Loading -> false
    }

private val RecipeListState.ingredientOptionsError: String?
    get() = when (this) {
        is RecipeListState.Content -> ingredientOptionsError
        is RecipeListState.Empty -> ingredientOptionsError
        is RecipeListState.Error -> ingredientOptionsError
        RecipeListState.Loading -> null
    }
