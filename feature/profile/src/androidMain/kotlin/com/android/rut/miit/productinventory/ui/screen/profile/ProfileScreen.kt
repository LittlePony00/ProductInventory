package com.android.rut.miit.productinventory.ui.screen.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.rut.miit.productinventory.core.R
import com.android.rut.miit.productinventory.feature.profile.presentation.*
import com.android.rut.miit.productinventory.ui.design.ScreenError
import com.android.rut.miit.productinventory.ui.design.ScreenLoading
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    householdId: String? = null,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(householdId) { viewModel.onEvent(ProfileEvent.OnCreate(householdId)) }

    LaunchedEffect(Unit) {
        viewModel.viewAction.collect { action ->
            when (action) {
                is ProfileAction.NavigateToLogin -> onNavigateToLogin()
                is ProfileAction.NavigateBack -> onBack()
                is ProfileAction.ShowMessage -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnBackClick) }) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is ProfileState.Loading -> {
                ScreenLoading(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is ProfileState.Content -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = s.profile.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                            }
                        }
                        Text(s.profile.name, style = MaterialTheme.typography.headlineSmall)
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.profile_email_label), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(s.profile.email, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.profile_name_label), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (s.isEditing) {
                                OutlinedTextField(
                                    value = s.editName,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnNameChanged(it)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnCancelEdit) }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { viewModel.onEvent(ProfileEvent.OnSaveClick) }) {
                                        Text(stringResource(R.string.save))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(s.profile.name, style = MaterialTheme.typography.bodyLarge)
                                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnEditClick) }) {
                                        Text(stringResource(R.string.profile_edit))
                                    }
                                }
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.profile_food_preferences), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.profile_food_preferences_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (s.isEditingFoodPreferences) {
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_preferred_cuisines),
                                    value = s.editPreferredCuisines,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnPreferredCuisinesChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_preferred_products_text),
                                    value = s.editPreferredProducts,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnPreferredProductsChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_disliked_ingredients),
                                    value = s.editDislikedIngredients,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnDislikedIngredientsChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_avoided_products_text),
                                    value = s.editAvoidedProducts,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnAvoidedProductsChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_allergies),
                                    value = s.editAllergies,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnAllergiesChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_dietary_restrictions),
                                    value = s.editDietaryRestrictions,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnDietaryRestrictionsChanged(it)) }
                                )
                                if (s.foodPreferenceOptions.hasStructuredOptions) {
                                    StructuredPreferenceChips(
                                        title = stringResource(R.string.profile_preferred_products_selected),
                                        options = s.foodPreferenceOptions.products.map { it.id to it.name },
                                        selectedIds = s.editPreferredProductIds,
                                        onToggle = { viewModel.onEvent(ProfileEvent.OnPreferredProductToggled(it)) }
                                    )
                                    StructuredPreferenceChips(
                                        title = stringResource(R.string.profile_avoided_products_selected),
                                        options = s.foodPreferenceOptions.products.map { it.id to it.name },
                                        selectedIds = s.editAvoidedProductIds,
                                        onToggle = { viewModel.onEvent(ProfileEvent.OnAvoidedProductToggled(it)) }
                                    )
                                    StructuredPreferenceChips(
                                        title = stringResource(R.string.profile_preferred_categories),
                                        options = s.foodPreferenceOptions.categories.map { it.id to it.name },
                                        selectedIds = s.editPreferredCategoryIds,
                                        onToggle = { viewModel.onEvent(ProfileEvent.OnPreferredCategoryToggled(it)) }
                                    )
                                    StructuredPreferenceChips(
                                        title = stringResource(R.string.profile_avoided_categories),
                                        options = s.foodPreferenceOptions.categories.map { it.id to it.name },
                                        selectedIds = s.editAvoidedCategoryIds,
                                        onToggle = { viewModel.onEvent(ProfileEvent.OnAvoidedCategoryToggled(it)) }
                                    )
                                }
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_max_cooking_time),
                                    value = s.editMaxCookingTimeMinutes,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnMaxCookingTimeChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_preferred_difficulty),
                                    value = s.editPreferredDifficulty,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnPreferredDifficultyChanged(it)) }
                                )
                                FoodPreferenceField(
                                    label = stringResource(R.string.profile_servings),
                                    value = s.editServings,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.OnServingsChanged(it)) }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnCancelFoodPreferencesEdit) }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = { viewModel.onEvent(ProfileEvent.OnSaveFoodPreferencesClick) }) {
                                        Text(stringResource(R.string.save))
                                    }
                                }
                            } else {
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_preferred_cuisines),
                                    value = s.foodPreferences.preferredCuisines.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_preferred_products_text),
                                    value = s.foodPreferences.preferredProducts.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_disliked_ingredients),
                                    value = s.foodPreferences.dislikedIngredients.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_avoided_products_text),
                                    value = s.foodPreferences.avoidedProducts.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_allergies),
                                    value = s.foodPreferences.allergies.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_dietary_restrictions),
                                    value = s.foodPreferences.dietaryRestrictions.joinToString().ifBlank {
                                        stringResource(R.string.profile_not_set)
                                    }
                                )
                                if (s.foodPreferenceOptions.hasStructuredOptions) {
                                    PreferenceSummaryRow(
                                        label = stringResource(R.string.profile_preferred_products_selected),
                                        value = s.productNames(s.foodPreferences.preferredProductIds)
                                    )
                                    PreferenceSummaryRow(
                                        label = stringResource(R.string.profile_avoided_products_selected),
                                        value = s.productNames(s.foodPreferences.avoidedProductIds)
                                    )
                                    PreferenceSummaryRow(
                                        label = stringResource(R.string.profile_preferred_categories),
                                        value = s.categoryNames(s.foodPreferences.preferredCategoryIds)
                                    )
                                    PreferenceSummaryRow(
                                        label = stringResource(R.string.profile_avoided_categories),
                                        value = s.categoryNames(s.foodPreferences.avoidedCategoryIds)
                                    )
                                }
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_max_cooking_time),
                                    value = s.foodPreferences.maxCookingTimeMinutes?.toString()
                                        ?: stringResource(R.string.profile_not_set)
                                )
                                PreferenceSummaryRow(
                                    label = stringResource(R.string.profile_servings),
                                    value = s.foodPreferences.servings?.toString()
                                        ?: stringResource(R.string.profile_not_set)
                                )
                                TextButton(onClick = { viewModel.onEvent(ProfileEvent.OnEditFoodPreferencesClick) }) {
                                    Text(stringResource(R.string.profile_edit))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showLogoutConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.profile_logout))
                    }
                }
            }
            is ProfileState.Error -> {
                ScreenError(
                    message = s.message ?: stringResource(R.string.error_loading),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { viewModel.onEvent(ProfileEvent.OnRetry) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.onEvent(ProfileEvent.OnLogoutClick)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FoodPreferenceField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StructuredPreferenceChips(
    title: String,
    options: List<Pair<String, String>>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        if (options.isEmpty()) {
            Text(stringResource(R.string.profile_not_set), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.first }) { (id, label) ->
                    FilterChip(
                        selected = id in selectedIds,
                        onClick = { onToggle(id) },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileState.Content.productNames(ids: Set<String>): String {
    val namesById = foodPreferenceOptions.products.associate { it.id to it.name }
    return ids.mapNotNull(namesById::get).sorted().joinToString().ifBlank {
        stringResource(R.string.profile_not_set)
    }
}

@Composable
private fun ProfileState.Content.categoryNames(ids: Set<String>): String {
    val namesById = foodPreferenceOptions.categories.associate { it.id to it.name }
    return ids.mapNotNull(namesById::get).sorted().joinToString().ifBlank {
        stringResource(R.string.profile_not_set)
    }
}

@Composable
private fun PreferenceSummaryRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
