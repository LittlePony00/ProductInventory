package com.android.rut.miit.productinventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.android.rut.miit.productinventory.feature.auth.api.RestoreSessionUseCase
import com.android.rut.miit.productinventory.feature.auth.api.ValidateSessionUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.products.api.OutboxSyncCoordinator
import com.android.rut.miit.productinventory.core.push.AndroidProductLocalReminderRescheduler
import com.android.rut.miit.productinventory.navigation.Route
import com.android.rut.miit.productinventory.ui.screen.auth.LoginScreen
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.ui.screen.auth.RegisterScreen
import com.android.rut.miit.productinventory.ui.screen.household.HouseholdListScreen
import com.android.rut.miit.productinventory.ui.screen.notifications.NotificationListScreen
import com.android.rut.miit.productinventory.ui.screen.products.AddProductScreen
import com.android.rut.miit.productinventory.ui.screen.products.CategoryManagementScreen
import com.android.rut.miit.productinventory.ui.screen.products.ProductListScreen
import com.android.rut.miit.productinventory.ui.screen.profile.ProfileScreen
import com.android.rut.miit.productinventory.ui.screen.barcode.BarcodeScannerScreen
import com.android.rut.miit.productinventory.ui.screen.recipes.RecipeListScreen
import com.android.rut.miit.productinventory.ui.design.ProductInventoryTheme
import com.android.rut.miit.productinventory.ui.notification.AndroidNotificationBootstrap
import org.koin.compose.koinInject

@Composable
fun App() {
    ProductInventoryTheme {
        val navController = rememberNavController()
        val restoreSessionUseCase = koinInject<RestoreSessionUseCase>()
        val validateSessionUseCase = koinInject<ValidateSessionUseCase>()
        val getHouseholdsUseCase = koinInject<GetHouseholdsUseCase>()
        val outboxSyncCoordinator = koinInject<OutboxSyncCoordinator>()
        val localReminderRescheduler = koinInject<AndroidProductLocalReminderRescheduler>()
        var isAuthenticated by rememberSaveable { mutableStateOf(false) }

        AndroidNotificationBootstrap(isAuthenticated = isAuthenticated)
        AuthenticatedOutboxBootstrap(
            isAuthenticated = isAuthenticated,
            getHouseholdsUseCase = getHouseholdsUseCase,
            outboxSyncCoordinator = outboxSyncCoordinator,
            localReminderRescheduler = localReminderRescheduler
        )

        NavHost(navController = navController, startDestination = Route.AuthBootstrap) {
            composable<Route.AuthBootstrap> {
                AuthBootstrapScreen(
                    onAuthenticated = {
                        isAuthenticated = true
                        navController.navigate(Route.HouseholdList) {
                            popUpTo(Route.AuthBootstrap) { inclusive = true }
                        }
                    },
                    onUnauthenticated = {
                        isAuthenticated = false
                        navController.navigate(Route.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    restoreSessionUseCase = restoreSessionUseCase,
                    validateSessionUseCase = validateSessionUseCase
                )
            }

            composable<Route.Login> {
                LoginScreen(
                    onNavigateToHome = {
                        isAuthenticated = true
                        navController.navigate(Route.HouseholdList) {
                            popUpTo(Route.Login) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Route.Register) }
                )
            }

            composable<Route.Register> {
                RegisterScreen(
                    onNavigateToHome = {
                        isAuthenticated = true
                        navController.navigate(Route.HouseholdList) {
                            popUpTo(Route.Login) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Route.HouseholdList> {
                HouseholdListScreen(
                    onNavigateToHousehold = { id -> navController.navigate(Route.ProductList(id)) },
                    onNavigateToProfile = { navController.navigate(Route.Profile) }
                )
            }

            composable<Route.ProductList> { entry ->
                val route = entry.toRoute<Route.ProductList>()
                ProductListScreen(
                    householdId = route.householdId,
                    onAddProduct = { navController.navigate(Route.AddProduct(route.householdId)) },
                    onEditProduct = { productId -> navController.navigate(Route.AddProduct(route.householdId, productId = productId)) },
                    onBack = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(Route.Categories(route.householdId)) },
                    onNavigateToRecipes = { navController.navigate(Route.Recipes(route.householdId)) },
                    onNavigateToNotifications = { navController.navigate(Route.Notifications) },
                    onNavigateToBarcodeScan = { navController.navigate(Route.BarcodeScan(route.householdId)) }
                )
            }

            composable<Route.AddProduct> { entry ->
                val route = entry.toRoute<Route.AddProduct>()
                AddProductScreen(
                    householdId = route.householdId,
                    productId = route.productId,
                    barcode = route.barcode,
                    initialName = route.name,
                    initialBrand = route.brand,
                    initialCategory = route.category,
                    initialPackageAmount = route.packageAmount,
                    initialPackageUnit = route.packageUnit,
                    initialIngredientsText = route.ingredientsText,
                    initialImageUrl = route.imageUrl,
                    initialLocalImagePath = route.localImagePath,
                    initialCalories = route.calories,
                    initialProtein = route.protein,
                    initialFat = route.fat,
                    initialCarbs = route.carbs,
                    onProductAdded = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.Recipes> { entry ->
                val route = entry.toRoute<Route.Recipes>()
                RecipeListScreen(
                    householdId = route.householdId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.Categories> { entry ->
                val route = entry.toRoute<Route.Categories>()
                CategoryManagementScreen(
                    householdId = route.householdId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.Notifications> {
                NotificationListScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.Profile> {
                ProfileScreen(
                    onNavigateToLogin = {
                        isAuthenticated = false
                        navController.navigate(Route.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.BarcodeScan> { entry ->
                val route = entry.toRoute<Route.BarcodeScan>()
                BarcodeScannerScreen(
                    householdId = route.householdId,
                    onBack = { navController.popBackStack() },
                    onManualEntry = { barcode ->
                        navController.navigate(Route.AddProduct(route.householdId, barcode = barcode))
                    },
                    onDraftEntry = { draft ->
                        navController.navigate(draft.toAddProductRoute(route.householdId))
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedOutboxBootstrap(
    isAuthenticated: Boolean,
    getHouseholdsUseCase: GetHouseholdsUseCase,
    outboxSyncCoordinator: OutboxSyncCoordinator,
    localReminderRescheduler: AndroidProductLocalReminderRescheduler
) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(isAuthenticated, getHouseholdsUseCase, outboxSyncCoordinator, localReminderRescheduler) {
        if (!isAuthenticated) return@LaunchedEffect
        runCatching { getHouseholdsUseCase() }
            .getOrDefault(emptyList())
            .forEach { household ->
                runCatching { outboxSyncCoordinator.sync(household.id) }
            }
        runCatching { localReminderRescheduler.reschedule(context) }
    }
}

private fun BarcodeProductDraft.toAddProductRoute(householdId: String): Route.AddProduct =
    Route.AddProduct(
        householdId = householdId,
        productId = null,
        barcode = barcode,
        name = name,
        brand = brand,
        category = category?.name,
        packageAmount = packageQuantity?.toString(),
        packageUnit = packageQuantityUnit?.name,
        ingredientsText = ingredients,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        calories = caloriesKcal?.toString(),
        protein = proteinGrams?.toString(),
        fat = fatGrams?.toString(),
        carbs = carbohydratesGrams?.toString()
    )

@Composable
private fun AuthBootstrapScreen(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    restoreSessionUseCase: RestoreSessionUseCase = koinInject(),
    validateSessionUseCase: ValidateSessionUseCase = koinInject()
) {
    LaunchedEffect(restoreSessionUseCase, validateSessionUseCase) {
        if (!restoreSessionUseCase()) {
            onUnauthenticated()
            return@LaunchedEffect
        }

        onAuthenticated()

        if (!validateSessionUseCase()) {
            onUnauthenticated()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
