package com.android.rut.miit.productinventory.ui.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.android.rut.miit.productinventory.feature.auth.api.AuthRepository
import com.android.rut.miit.productinventory.feature.auth.api.LoginUseCase
import com.android.rut.miit.productinventory.feature.auth.api.models.AuthToken
import com.android.rut.miit.productinventory.feature.auth.presentation.login.LoginViewModel
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.ConsumeProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.presentation.list.ProductListViewModel
import com.android.rut.miit.productinventory.feature.household.api.CreateHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.GenerateInviteCodeUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.HouseholdRepository
import com.android.rut.miit.productinventory.feature.household.api.JoinHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.RefreshHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.api.models.InviteCode
import com.android.rut.miit.productinventory.feature.household.api.models.Member
import com.android.rut.miit.productinventory.feature.household.presentation.list.HouseholdListViewModel
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkAllReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.MarkNotificationReadUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.UpdateNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.notifications.presentation.NotificationListViewModel
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferenceOptionsUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.UpdateFoodPreferencesUseCase
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferenceOptions
import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import com.android.rut.miit.productinventory.feature.profile.api.models.UserProfile
import com.android.rut.miit.productinventory.feature.profile.presentation.ProfileViewModel
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.RealtimeRepository
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import com.android.rut.miit.productinventory.feature.recommendations.api.FindRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetLikedRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeIngredientOptionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.SetRecipeLikedUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.models.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeSearchRequest
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecommendationMode
import com.android.rut.miit.productinventory.feature.recommendations.api.models.localIdentity
import com.android.rut.miit.productinventory.ui.screen.household.HouseholdListScreen
import com.android.rut.miit.productinventory.feature.recommendations.presentation.RecipeListViewModel
import com.android.rut.miit.productinventory.ui.screen.auth.LoginScreen
import com.android.rut.miit.productinventory.ui.screen.notifications.NotificationListScreen
import com.android.rut.miit.productinventory.ui.screen.profile.ProfileScreen
import com.android.rut.miit.productinventory.ui.screen.products.ProductListScreen
import com.android.rut.miit.productinventory.ui.screen.recipes.RecipeListScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlatformScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        runCatching { stopKoin() }
    }

    @After
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun recipeScreenRendersEmptyState() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = recipeViewModel(FakeRecipeRepository())
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Нет подходящих рецептов").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Рецепты").assertIsDisplayed()
        compose.onAllNodesWithText("Найти рецепт")[0].assertIsDisplayed()
        compose.onNodeWithText("Нет подходящих рецептов").assertIsDisplayed()
    }

    @Test
    fun recipeScreenHidesAiBadgesOnRecipeCards() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = recipeViewModel(
                        FakeRecipeRepository(
                            recipes = listOf(
                                Recipe(
                                    title = "AI-assisted rice",
                                    ingredients = listOf(RecipeIngredient("rice", "1 cup")),
                                    steps = listOf("Cook rice"),
                                    time = "15 минут",
                                    calories = 0,
                                    caloriesKnown = false,
                                    source = "AI_ASSISTED",
                                    aiAssisted = true
                                )
                            )
                        )
                    )
                )
            }
        }

        compose.onNodeWithText("AI-assisted rice").assertIsDisplayed()
        compose.onNodeWithText("15 минут • ккал неизвестно").assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithText("AI-Assisted").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("ИИ-рецепт").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("ИИ").fetchSemanticsNodes().size)
    }

    @Test
    fun recipeScreenLikesRecipeAndShowsLikedTab() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = recipeViewModel(
                        FakeRecipeRepository(
                            recipes = listOf(
                                Recipe(
                                    title = "Рис с овощами",
                                    ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                                    steps = listOf("Сварить рис"),
                                    time = "20 минут",
                                    calories = 300
                                )
                            )
                        )
                    )
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("♡ В понравившиеся").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("♡ В понравившиеся").performClick()
        compose.onNodeWithText("Понравившиеся").performClick()
        compose.onNodeWithText("Рис с овощами").assertIsDisplayed()
        compose.onNodeWithText("♥ Убрать").assertIsDisplayed()
    }

    @Test
    fun recipeIngredientDialogUsesPluralRandomRecipesAction() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = recipeViewModel(FakeRecipeRepository())
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Нет подходящих рецептов").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Найти рецепт")[0].performClick()
        compose.onNodeWithText("Рандомные рецепты").assertIsDisplayed()
    }

    @Test
    fun profileScreenUsesShortFoodPreferenceLabelsInSummaryAndEditMode() {
        compose.setContent {
            MaterialTheme {
                ProfileScreen(
                    householdId = "household-id",
                    onNavigateToLogin = {},
                    onBack = {},
                    viewModel = profileViewModel(
                        FakeProfileRepository(
                            foodPreferences = FoodPreferences(
                                preferredProducts = setOf("рис"),
                                avoidedProducts = setOf("майонез")
                            )
                        )
                    )
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Пищевые предпочтения").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(compose.onAllNodesWithText("Любимые продукты").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Продукты, которых избегать").fetchSemanticsNodes().isNotEmpty())
        assertEquals(0, compose.onAllNodesWithText("(текстом)", substring = true).fetchSemanticsNodes().size)

        compose.onAllNodesWithText("Изменить")[1].performScrollTo().performClick()

        assertTrue(compose.onAllNodesWithText("Любимые продукты").fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Продукты, которых избегать").fetchSemanticsNodes().isNotEmpty())
        assertEquals(0, compose.onAllNodesWithText("(текстом)", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun recipeScreenRendersErrorState() {
        compose.setContent {
            MaterialTheme {
                RecipeListScreen(
                    householdId = "household-id",
                    onBack = {},
                    viewModel = recipeViewModel(
                        FakeRecipeRepository(error = IllegalStateException("recipe backend down"))
                    )
                )
            }
        }

        compose.onNodeWithText("recipe backend down").assertIsDisplayed()
        compose.onNodeWithText("Повторить").assertIsDisplayed()
    }

    @Test
    fun householdJoinDialogKeepsInvalidInviteErrorVisible() {
        compose.setContent {
            MaterialTheme {
                HouseholdListScreen(
                    onNavigateToHousehold = {},
                    onNavigateToProfile = {},
                    viewModel = householdListViewModel(
                        FakeHouseholdRepository(
                            joinError = IllegalStateException("InviteCode with id 'BADCODE' not found")
                        )
                    )
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Нет домохозяйств").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Вступить").performClick()
        compose.onNodeWithText("Код приглашения").performTextInput("BADCODE")
        compose.onAllNodesWithText("Вступить")[1].performClick()
        compose.waitUntil {
            compose.onAllNodesWithText("InviteCode with id 'BADCODE' not found").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Присоединиться").assertIsDisplayed()
        compose.onAllNodesWithText("InviteCode with id 'BADCODE' not found")[0].assertIsDisplayed()
    }

    @Test
    fun notificationScreenRendersSettingsAndEmptyState() {
        compose.setContent {
            MaterialTheme {
                NotificationListScreen(
                    onBack = {},
                    viewModel = notificationViewModel(FakeNotificationRepository())
                )
            }
        }

        compose.onNodeWithText("Настройки уведомлений").assertIsDisplayed()
        compose.onNodeWithText("Напоминать о сроке годности").assertIsDisplayed()
        compose.onNodeWithText("Напоминать о низком запасе").assertIsDisplayed()
        compose.onNodeWithText("Push-уведомления").assertIsDisplayed()
        compose.onNodeWithText("Нет уведомлений").assertIsDisplayed()
    }

    @Test
    fun productListConsumeDialogSubmitsPartialConsumption() {
        val productRepository = FakeProductRepository(
            initialProducts = listOf(product(remainingAmount = 2.0))
        )

        compose.setContent {
            MaterialTheme {
                ProductListScreen(
                    householdId = "household-id",
                    onAddProduct = {},
                    onEditProduct = {},
                    onBack = {},
                    onManageCategories = {},
                    onNavigateToRecipes = {},
                    onNavigateToNotifications = {},
                    onNavigateToBarcodeScan = {},
                    viewModel = productListViewModel(productRepository)
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Списать").performClick()
        compose.onNodeWithText("Списать продукт").assertIsDisplayed()
        compose.onNodeWithText("Количество для списания").performTextClearance()
        compose.onNodeWithText("Количество для списания").performTextInput("0.75")
        compose.onAllNodesWithText("Списать")[1].performClick()
        compose.waitUntil {
            productRepository.consumeCalls.singleOrNull()?.amount == 0.75
        }
        compose.waitUntil {
            compose.onAllNodesWithText("Остаток: 1", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Остаток: 1", substring = true).assertIsDisplayed()
    }

    @Test
    fun productListFullConsumptionDisablesConsumeAction() {
        val productRepository = FakeProductRepository(
            initialProducts = listOf(product(remainingAmount = 1.0))
        )

        compose.setContent {
            MaterialTheme {
                ProductListScreen(
                    householdId = "household-id",
                    onAddProduct = {},
                    onEditProduct = {},
                    onBack = {},
                    onManageCategories = {},
                    onNavigateToRecipes = {},
                    onNavigateToNotifications = {},
                    onNavigateToBarcodeScan = {},
                    viewModel = productListViewModel(productRepository)
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Списать").performClick()
        compose.onAllNodesWithText("Списать")[1].performClick()
        compose.waitUntil {
            productRepository.consumeCalls.singleOrNull()?.amount == 1.0
        }
        compose.waitUntil {
            compose.onAllNodesWithText("Остаток: 0", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Списать").assertIsNotEnabled()
    }

    @Test
    fun productListEmptyStateStillExposesRecipeNavigation() {
        var recipeNavigationRequested = false

        compose.setContent {
            MaterialTheme {
                ProductListScreen(
                    householdId = "household-id",
                    onAddProduct = {},
                    onEditProduct = {},
                    onBack = {},
                    onManageCategories = {},
                    onNavigateToRecipes = { recipeNavigationRequested = true },
                    onNavigateToNotifications = {},
                    onNavigateToBarcodeScan = {},
                    viewModel = productListViewModel(FakeProductRepository(initialProducts = emptyList()))
                )
            }
        }

        compose.waitUntil {
            compose.onAllNodesWithText("Нет продуктов").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Рецепты").performClick()

        assertTrue(recipeNavigationRequested)
    }

    @Test
    fun authenticatedNavigationSmokeCoversHouseholdProductsRecipesAndNotifications() {
        val householdRepository = FakeHouseholdRepository(
            households = listOf(Household(id = "household-id", name = "Home", createdAt = "2026-05-15T00:00:00Z"))
        )
        val productRepository = FakeProductRepository(
            initialProducts = listOf(product(remainingAmount = 2.0))
        )
        var route by mutableStateOf("login")

        compose.setContent {
            MaterialTheme {
                when (route) {
                    "login" -> LoginScreen(
                        onNavigateToHome = { route = "households" },
                        onNavigateToRegister = {},
                        viewModel = LoginViewModel(LoginUseCase(FakeAuthRepository()))
                    )
                    "households" -> HouseholdListScreen(
                        onNavigateToHousehold = { route = "products" },
                        onNavigateToProfile = {},
                        viewModel = householdListViewModel(householdRepository)
                    )
                    "products" -> ProductListScreen(
                        householdId = "household-id",
                        onAddProduct = {},
                        onEditProduct = {},
                        onBack = { route = "households" },
                        onManageCategories = {},
                        onNavigateToRecipes = { route = "recipes" },
                        onNavigateToNotifications = { route = "notifications" },
                        onNavigateToBarcodeScan = {},
                        viewModel = productListViewModel(productRepository)
                    )
                    "recipes" -> RecipeListScreen(
                        householdId = "household-id",
                        onBack = { route = "products" },
                        viewModel = recipeViewModel(FakeRecipeRepository())
                    )
                    "notifications" -> NotificationListScreen(
                        onBack = { route = "products" },
                        viewModel = notificationViewModel(FakeNotificationRepository())
                    )
                }
            }
        }

        compose.onNodeWithText("Эл. почта").performTextInput("ui@test.com")
        compose.onNodeWithText("Пароль").performTextInput("password")
        compose.onNodeWithText("Войти").performClick()

        compose.waitUntil {
            compose.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Открыть").performClick()

        compose.waitUntil {
            compose.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Продукты").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertIsDisplayed()

        compose.onNodeWithText("Рецепты").performClick()
        compose.waitUntil {
            compose.onAllNodesWithText("Нет подходящих рецептов").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Назад").performClick()

        compose.waitUntil {
            compose.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Уведомления").performClick()
        compose.waitUntil {
            compose.onAllNodesWithText("Настройки уведомлений").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Нет уведомлений").assertIsDisplayed()
    }

    private fun notificationViewModel(repository: NotificationRepository): NotificationListViewModel =
        NotificationListViewModel(
            getNotificationsUseCase = GetNotificationsUseCase(repository),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(repository),
            markNotificationReadUseCase = MarkNotificationReadUseCase(repository),
            markAllReadUseCase = MarkAllReadUseCase(repository),
            updateNotificationSettingsUseCase = UpdateNotificationSettingsUseCase(repository)
        )

    private fun recipeViewModel(repository: RecipeRepository): RecipeListViewModel =
        RecipeListViewModel(
            getRecipesUseCase = GetRecipesUseCase(repository),
            getRecipeSuggestionsUseCase = GetRecipeSuggestionsUseCase(repository),
            getRecipeIngredientOptionsUseCase = GetRecipeIngredientOptionsUseCase(repository),
            findRecipesUseCase = FindRecipesUseCase(repository),
            getLikedRecipesUseCase = GetLikedRecipesUseCase(repository),
            setRecipeLikedUseCase = SetRecipeLikedUseCase(repository)
        )

    private fun profileViewModel(repository: ProfileRepository): ProfileViewModel =
        ProfileViewModel(
            getProfileUseCase = GetProfileUseCase(repository),
            getFoodPreferencesUseCase = GetFoodPreferencesUseCase(repository),
            getFoodPreferenceOptionsUseCase = GetFoodPreferenceOptionsUseCase(repository),
            updateProfileUseCase = UpdateProfileUseCase(repository),
            updateFoodPreferencesUseCase = UpdateFoodPreferencesUseCase(repository),
            tokenStorage = FakeTokenStorage()
        )

    private fun householdListViewModel(repository: HouseholdRepository): HouseholdListViewModel =
        HouseholdListViewModel(
            getHouseholdsUseCase = GetHouseholdsUseCase(repository),
            refreshHouseholdsUseCase = RefreshHouseholdsUseCase(repository),
            createHouseholdUseCase = CreateHouseholdUseCase(repository),
            generateInviteCodeUseCase = GenerateInviteCodeUseCase(repository),
            joinHouseholdUseCase = JoinHouseholdUseCase(repository)
        )

    private fun productListViewModel(repository: ProductRepository): ProductListViewModel =
        ProductListViewModel(
            getProductsUseCase = GetProductsUseCase(repository),
            refreshProductsUseCase = RefreshProductsUseCase(repository),
            getProductCategoriesUseCase = GetProductCategoriesUseCase(FakeCategoryRepository()),
            refreshProductCategoriesUseCase = RefreshProductCategoriesUseCase(FakeCategoryRepository()),
            deleteProductUseCase = DeleteProductUseCase(repository),
            consumeProductUseCase = ConsumeProductUseCase(repository),
            applyRealtimeProductEventUseCase = ApplyRealtimeProductEventUseCase(repository),
            observeHouseholdEventsUseCase = ObserveHouseholdEventsUseCase(FakeRealtimeRepository()),
            getNotificationSettingsUseCase = GetNotificationSettingsUseCase(FakeNotificationRepository())
        )

    private fun product(remainingAmount: Double): Product =
        Product(
            id = "product-id",
            name = "Milk",
            category = ProductCategory.DAIRY,
            categoryId = ProductCategoryOption.DAIRY_SYSTEM_ID,
            categoryName = "Dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            remainingAmount = remainingAmount,
            expirationDate = null,
            expirationStatus = ExpirationStatus.FRESH,
            householdId = "household-id",
            addedByUserId = "user-id",
            createdAt = "2026-05-14T00:00:00Z"
        )

    private class FakeRecipeRepository(
        private val recipes: List<Recipe> = emptyList(),
        private val suggestions: List<Recipe> = emptyList(),
        private val error: Throwable? = null
    ) : RecipeRepository {
        private var likedRecipes = emptyList<Recipe>()

        override suspend fun getRecipes(householdId: String, mode: RecommendationMode): List<Recipe> {
            error?.let { throw it }
            return recipes
        }

        override suspend fun getRecipeSuggestions(householdId: String): List<Recipe> {
            error?.let { throw it }
            return suggestions
        }

        override suspend fun getIngredientOptions(householdId: String): List<RecipeIngredientOption> =
            emptyList()

        override suspend fun findRecipes(householdId: String, request: RecipeSearchRequest): List<Recipe> =
            recipes

        override suspend fun generateAiRecipe(householdId: String, request: AiRecipeGenerationRequest): Recipe =
            recipes.firstOrNull() ?: Recipe(
                title = "ИИ-рецепт",
                ingredients = emptyList(),
                steps = emptyList(),
                time = "",
                calories = 0,
                aiGenerated = true
            )

        override suspend fun getLikedRecipes(householdId: String): List<Recipe> =
            likedRecipes

        override suspend fun setRecipeLiked(householdId: String, recipe: Recipe, liked: Boolean): List<Recipe> {
            likedRecipes = if (liked) {
                likedRecipes.filterNot { it.localIdentity() == recipe.localIdentity() } + recipe
            } else {
                likedRecipes.filterNot { it.localIdentity() == recipe.localIdentity() }
            }
            return likedRecipes
        }
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun register(email: String, password: String, name: String): AuthToken =
            AuthToken(accessToken = "access", refreshToken = "refresh", userId = "user-id")

        override suspend fun login(email: String, password: String): AuthToken =
            AuthToken(accessToken = "access", refreshToken = "refresh", userId = "user-id")

        override suspend fun logout() = Unit
        override fun isLoggedIn(): Boolean = false
    }

    private class FakeHouseholdRepository(
        private val households: List<Household> = emptyList(),
        private val joinError: Throwable? = null
    ) : HouseholdRepository {
        override suspend fun getMyHouseholds(): List<Household> = households
        override suspend fun getHousehold(householdId: String): Household =
            Household(id = householdId, name = "Home", createdAt = "2026-05-15T00:00:00Z")

        override suspend fun createHousehold(name: String): Household =
            Household(id = "created-id", name = name, createdAt = "2026-05-15T00:00:00Z")

        override suspend fun getMembers(householdId: String): List<Member> = emptyList()
        override suspend fun generateInviteCode(householdId: String): InviteCode =
            InviteCode(code = "INVITE42", expiresAt = "2026-05-22T00:00:00Z")

        override suspend fun joinByInviteCode(inviteCode: String): Household {
            joinError?.let { throw it }
            return Household(id = "joined-id", name = "Joined", createdAt = "2026-05-15T00:00:00Z")
        }

        override suspend fun removeMember(householdId: String, memberId: String) = Unit
        override suspend fun leaveHousehold(householdId: String) = Unit
    }

    private class FakeNotificationRepository : NotificationRepository {
        override suspend fun getNotifications(): List<Notification> = emptyList()
        override suspend fun getUnreadNotifications(): List<Notification> = emptyList()
        override suspend fun markAsRead(notificationId: String) = Unit
        override suspend fun markAllAsRead() = Unit
        override suspend fun getSettings(): NotificationSettings =
            NotificationSettings(pushEnabled = false)

        override suspend fun updateSettings(
            expirationRemindersEnabled: Boolean?,
            lowStockRemindersEnabled: Boolean?,
            pushEnabled: Boolean?,
            expirationReminderDays: Int?
        ): NotificationSettings =
            getSettings()
    }

    private class FakeProfileRepository(
        private var foodPreferences: FoodPreferences = FoodPreferences()
    ) : ProfileRepository {
        private var profile = UserProfile(id = "user-id", email = "user@example.com", name = "User")

        override suspend fun getProfile(): UserProfile = profile

        override suspend fun updateProfile(name: String): UserProfile {
            profile = profile.copy(name = name)
            return profile
        }

        override suspend fun getFoodPreferences(): FoodPreferences = foodPreferences

        override suspend fun getFoodPreferenceOptions(householdId: String): FoodPreferenceOptions =
            FoodPreferenceOptions()

        override suspend fun updateFoodPreferences(preferences: FoodPreferences): FoodPreferences {
            foodPreferences = preferences
            return preferences
        }
    }

    private class FakeTokenStorage : TokenStorage {
        private var accessToken: String? = null
        private var refreshToken: String? = null
        private var userId: String? = null

        override fun getAccessToken(): String? = accessToken
        override fun getRefreshToken(): String? = refreshToken
        override fun saveTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
        }
        override fun clearTokens() {
            accessToken = null
            refreshToken = null
            userId = null
        }
        override fun getUserId(): String? = userId
        override fun saveUserId(userId: String) {
            this.userId = userId
        }
    }

    private class FakeProductRepository(initialProducts: List<Product>) : ProductRepository {
        private val products = initialProducts.associateBy { it.id }.toMutableMap()
        val consumeCalls = mutableListOf<ConsumeCall>()

        override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> =
            products.values.filter { product ->
                product.householdId == householdId && (categoryId == null || product.categoryId == categoryId)
            }

        override suspend fun getProduct(householdId: String, productId: String): Product =
            products.getValue(productId)

        override suspend fun addProduct(
            householdId: String,
            name: String,
            category: ProductCategory,
            categoryId: String?,
            quantity: Double,
            quantityUnit: QuantityUnit,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product = error("Unused")

        override suspend fun updateProduct(
            householdId: String,
            productId: String,
            name: String?,
            category: ProductCategory?,
            categoryId: String?,
            quantity: Double?,
            quantityUnit: QuantityUnit?,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product = error("Unused")

        override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product {
            consumeCalls += ConsumeCall(householdId, productId, amount)
            val product = products.getValue(productId)
            return product.copy(remainingAmount = product.remainingAmount - amount)
                .also { products[productId] = it }
        }

        override suspend fun deleteProduct(householdId: String, productId: String) {
            products.remove(productId)
        }

        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> =
            emptyList()

        override suspend fun suggestProductEnrichment(
            householdId: String,
            name: String?,
            brand: String?,
            barcode: String?,
            ingredientsText: String?
        ): ProductEnrichmentSuggestion = error("Unused")

        override suspend fun upsertCachedProduct(product: Product) {
            products[product.id] = product
        }

        override suspend fun deleteCachedProduct(productId: String) {
            products.remove(productId)
        }
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            ProductCategoryOption.systemDefaults()

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
            error("Unused")

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
            error("Unused")

        override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
    }

    private class FakeRealtimeRepository : RealtimeRepository {
        override fun observeHouseholdEvents(householdId: String): Flow<HouseholdRealtimeEvent> =
            emptyFlow()
    }

    private data class ConsumeCall(
        val householdId: String,
        val productId: String,
        val amount: Double
    )
}
