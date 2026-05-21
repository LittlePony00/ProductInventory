package com.android.rut.miit.productinventory.feature.profile.presentation

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferenceOptionsUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.UpdateFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val getFoodPreferencesUseCase: GetFoodPreferencesUseCase,
    private val getFoodPreferenceOptionsUseCase: GetFoodPreferenceOptionsUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val updateFoodPreferencesUseCase: UpdateFoodPreferencesUseCase,
    private val tokenStorage: TokenStorage
) : SharedViewModel<ProfileState, ProfileEvent, ProfileAction>(
    initialState = ProfileState.Loading
) {
    private var currentHouseholdId: String? = null

    override suspend fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnCreate -> loadProfile(event.householdId)
            is ProfileEvent.OnRetry -> loadProfile(currentHouseholdId)
            is ProfileEvent.OnEditClick -> onEditClick()
            is ProfileEvent.OnNameChanged -> onNameChanged(event.name)
            is ProfileEvent.OnSaveClick -> onSave()
            is ProfileEvent.OnCancelEdit -> onCancelEdit()
            is ProfileEvent.OnEditFoodPreferencesClick -> onEditFoodPreferencesClick()
            is ProfileEvent.OnPreferredCuisinesChanged -> updateFoodEdit { copy(editPreferredCuisines = event.value) }
            is ProfileEvent.OnPreferredProductsChanged -> updateFoodEdit { copy(editPreferredProducts = event.value) }
            is ProfileEvent.OnDislikedIngredientsChanged -> updateFoodEdit { copy(editDislikedIngredients = event.value) }
            is ProfileEvent.OnAvoidedProductsChanged -> updateFoodEdit { copy(editAvoidedProducts = event.value) }
            is ProfileEvent.OnAllergiesChanged -> updateFoodEdit { copy(editAllergies = event.value) }
            is ProfileEvent.OnDietaryRestrictionsChanged -> updateFoodEdit { copy(editDietaryRestrictions = event.value) }
            is ProfileEvent.OnPreferredProductToggled -> updateFoodEdit {
                copy(
                    editPreferredProductIds = editPreferredProductIds.toggle(event.id),
                    editAvoidedProductIds = editAvoidedProductIds - event.id
                )
            }
            is ProfileEvent.OnAvoidedProductToggled -> updateFoodEdit {
                copy(
                    editAvoidedProductIds = editAvoidedProductIds.toggle(event.id),
                    editPreferredProductIds = editPreferredProductIds - event.id
                )
            }
            is ProfileEvent.OnPreferredCategoryToggled -> updateFoodEdit {
                copy(
                    editPreferredCategoryIds = editPreferredCategoryIds.toggle(event.id),
                    editAvoidedCategoryIds = editAvoidedCategoryIds - event.id
                )
            }
            is ProfileEvent.OnAvoidedCategoryToggled -> updateFoodEdit {
                copy(
                    editAvoidedCategoryIds = editAvoidedCategoryIds.toggle(event.id),
                    editPreferredCategoryIds = editPreferredCategoryIds - event.id
                )
            }
            is ProfileEvent.OnMaxCookingTimeChanged -> updateFoodEdit { copy(editMaxCookingTimeMinutes = event.value) }
            is ProfileEvent.OnPreferredDifficultyChanged -> updateFoodEdit { copy(editPreferredDifficulty = event.value) }
            is ProfileEvent.OnServingsChanged -> updateFoodEdit { copy(editServings = event.value) }
            is ProfileEvent.OnSaveFoodPreferencesClick -> onSaveFoodPreferences()
            is ProfileEvent.OnCancelFoodPreferencesEdit -> onCancelFoodPreferencesEdit()
            is ProfileEvent.OnLogoutClick -> onLogout()
            is ProfileEvent.OnBackClick -> sendAction(ProfileAction.NavigateBack)
        }
    }

    private fun loadProfile(householdId: String?) {
        currentHouseholdId = householdId
        viewModelScope.launch {
            updateState { ProfileState.Loading }
            runCatching {
                val profile = getProfileUseCase()
                val foodPreferences = getFoodPreferencesUseCase()
                val options = householdId?.let { id ->
                    runCatching { getFoodPreferenceOptionsUseCase(id) }.getOrDefault(FoodPreferenceOptions())
                } ?: FoodPreferenceOptions()
                Triple(profile, foodPreferences, options)
            }
                .onSuccess { (profile, foodPreferences, options) ->
                    updateState {
                        ProfileState.Content(
                            profile = profile,
                            foodPreferences = foodPreferences,
                            foodPreferenceOptions = options,
                            householdId = householdId
                        )
                    }
                }
                .onFailure { error ->
                    updateState { ProfileState.Error(error.message) }
                }
        }
    }

    private fun onEditClick() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState {
                ProfileState.Content(
                    profile = state.profile,
                    foodPreferences = state.foodPreferences,
                    foodPreferenceOptions = state.foodPreferenceOptions,
                    householdId = state.householdId,
                    isEditing = true,
                    editName = state.profile.name
                )
            }
        }
    }

    private fun onNameChanged(name: String) {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.copy(editName = name) }
        }
    }

    private fun onSave() {
        val state = currentState
        if (state is ProfileState.Content && state.isEditing) {
            viewModelScope.launch {
                runCatching { updateProfileUseCase(state.editName) }
                    .onSuccess { profile ->
                        updateState { state.copy(profile = profile, isEditing = false) }
                        sendAction(ProfileAction.ShowMessage("Профиль обновлён"))
                    }
                    .onFailure { error ->
                        sendAction(ProfileAction.ShowMessage(error.message ?: "Ошибка"))
                    }
            }
        }
    }

    private fun onCancelEdit() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.copy(isEditing = false) }
        }
    }

    private fun onEditFoodPreferencesClick() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState {
                state.copy(
                    isEditingFoodPreferences = true,
                    editPreferredCuisines = state.foodPreferences.preferredCuisines.joinToString(),
                    editPreferredProducts = state.foodPreferences.preferredProducts.joinToString(),
                    editDislikedIngredients = state.foodPreferences.dislikedIngredients.joinToString(),
                    editAvoidedProducts = state.foodPreferences.avoidedProducts.joinToString(),
                    editAllergies = state.foodPreferences.allergies.joinToString(),
                    editDietaryRestrictions = state.foodPreferences.dietaryRestrictions.joinToString(),
                    editPreferredProductIds = state.foodPreferences.preferredProductIds,
                    editAvoidedProductIds = state.foodPreferences.avoidedProductIds,
                    editPreferredCategoryIds = state.foodPreferences.preferredCategoryIds,
                    editAvoidedCategoryIds = state.foodPreferences.avoidedCategoryIds,
                    editMaxCookingTimeMinutes = state.foodPreferences.maxCookingTimeMinutes?.toString().orEmpty(),
                    editPreferredDifficulty = state.foodPreferences.preferredDifficulty.orEmpty(),
                    editServings = state.foodPreferences.servings?.toString().orEmpty()
                )
            }
        }
    }

    private fun updateFoodEdit(reducer: ProfileState.Content.() -> ProfileState.Content) {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.reducer() }
        }
    }

    private fun onSaveFoodPreferences() {
        val state = currentState
        if (state is ProfileState.Content && state.isEditingFoodPreferences) {
            val preferences = runCatching { state.toEditedFoodPreferences() }
                .onFailure { sendAction(ProfileAction.ShowMessage(it.message ?: "Ошибка")) }
                .getOrNull() ?: return
            viewModelScope.launch {
                runCatching { updateFoodPreferencesUseCase(preferences) }
                    .onSuccess { updated ->
                        updateState {
                            state.copy(
                                foodPreferences = updated,
                                isEditingFoodPreferences = false
                            )
                        }
                        sendAction(ProfileAction.ShowMessage("Пищевые предпочтения обновлены"))
                    }
                    .onFailure { error ->
                        sendAction(ProfileAction.ShowMessage(error.message ?: "Ошибка"))
                    }
            }
        }
    }

    private fun onCancelFoodPreferencesEdit() {
        val state = currentState
        if (state is ProfileState.Content) {
            updateState { state.copy(isEditingFoodPreferences = false) }
        }
    }

    private fun onLogout() {
        viewModelScope.launch {
            tokenStorage.clearTokens()
            sendAction(ProfileAction.NavigateToLogin)
        }
    }
}

private fun ProfileState.Content.toEditedFoodPreferences(): FoodPreferences =
    FoodPreferences(
        preferredCuisines = editPreferredCuisines.csvSet(),
        preferredProducts = editPreferredProducts.csvSet(),
        dislikedIngredients = editDislikedIngredients.csvSet(),
        avoidedProducts = editAvoidedProducts.csvSet(),
        allergies = editAllergies.csvSet(),
        dietaryRestrictions = editDietaryRestrictions.enumSet(
            fieldName = "Ограничения питания",
            supportedValues = SUPPORTED_DIETARY_RESTRICTIONS
        ),
        preferredProductIds = editPreferredProductIds,
        avoidedProductIds = editAvoidedProductIds,
        preferredCategoryIds = editPreferredCategoryIds,
        avoidedCategoryIds = editAvoidedCategoryIds,
        maxCookingTimeMinutes = editMaxCookingTimeMinutes.optionalPositiveInt("Максимальное время"),
        preferredDifficulty = editPreferredDifficulty.optionalEnum(
            fieldName = "Сложность",
            supportedValues = SUPPORTED_DIFFICULTIES
        ),
        servings = editServings.optionalPositiveInt("Порции")
    )

private fun String.csvSet(): Set<String> =
    split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .toSet()

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun String.optionalPositiveInt(fieldName: String): Int? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    val value = trimmed.toIntOrNull() ?: error("$fieldName должно быть числом")
    require(value > 0) { "$fieldName должно быть больше 0" }
    return value
}

private fun String.enumSet(fieldName: String, supportedValues: Set<String>): Set<String> =
    csvSet().map { value ->
        DIETARY_RESTRICTION_ALIASES[value.normalizedPreferenceToken()] ?: value.uppercase().also { normalized ->
            require(normalized in supportedValues) {
                unsupportedPreferenceMessage(fieldName, supportedValues)
            }
        }
    }.toSet()

private fun String.optionalEnum(fieldName: String, supportedValues: Set<String>): String? {
    val value = trim().takeIf(String::isNotEmpty) ?: return null
    val normalized = DIFFICULTY_ALIASES[value.normalizedPreferenceToken()] ?: value.uppercase()
    require(normalized in supportedValues) {
        unsupportedPreferenceMessage(fieldName, supportedValues)
    }
    return normalized
}

private fun unsupportedPreferenceMessage(fieldName: String, supportedValues: Set<String>): String =
    when (fieldName) {
        "Ограничения питания" ->
            "$fieldName: введите вегетарианство, веганство, без глютена, без молочного, без орехов, халяль или кошерно"
        "Сложность" -> "$fieldName: введите легко, средне или сложно"
        else -> "$fieldName: поддерживаются ${supportedValues.joinToString()}"
    }

private fun String.normalizedPreferenceToken(): String =
    trim()
        .lowercase()
        .replace("ё", "е")
        .replace("-", " ")
        .replace("_", " ")
        .replace(Regex("\\s+"), " ")

private val SUPPORTED_DIETARY_RESTRICTIONS = setOf(
    "VEGETARIAN",
    "VEGAN",
    "GLUTEN_FREE",
    "DAIRY_FREE",
    "NUT_FREE",
    "HALAL",
    "KOSHER"
)

private val SUPPORTED_DIFFICULTIES = setOf("EASY", "MEDIUM", "HARD")

private val DIETARY_RESTRICTION_ALIASES = mapOf(
    "vegetarian" to "VEGETARIAN",
    "вегетарианство" to "VEGETARIAN",
    "вегетарианское" to "VEGETARIAN",
    "вегетарианская" to "VEGETARIAN",
    "vegan" to "VEGAN",
    "веганство" to "VEGAN",
    "веганское" to "VEGAN",
    "веганская" to "VEGAN",
    "gluten free" to "GLUTEN_FREE",
    "без глютена" to "GLUTEN_FREE",
    "dairy free" to "DAIRY_FREE",
    "без молочного" to "DAIRY_FREE",
    "без молока" to "DAIRY_FREE",
    "nut free" to "NUT_FREE",
    "без орехов" to "NUT_FREE",
    "halal" to "HALAL",
    "халяль" to "HALAL",
    "kosher" to "KOSHER",
    "кошерно" to "KOSHER",
    "кошерное" to "KOSHER"
)

private val DIFFICULTY_ALIASES = mapOf(
    "easy" to "EASY",
    "легко" to "EASY",
    "легкая" to "EASY",
    "простой" to "EASY",
    "простая" to "EASY",
    "medium" to "MEDIUM",
    "средне" to "MEDIUM",
    "средняя" to "MEDIUM",
    "hard" to "HARD",
    "сложно" to "HARD",
    "сложная" to "HARD"
)
