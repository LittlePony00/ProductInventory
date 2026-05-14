package com.android.rut.miit.productinventory

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.android.rut.miit.productinventory.navigation.Route
import com.android.rut.miit.productinventory.ui.screen.auth.LoginScreen
import com.android.rut.miit.productinventory.ui.screen.auth.RegisterScreen
import com.android.rut.miit.productinventory.ui.screen.household.HouseholdListScreen
import com.android.rut.miit.productinventory.ui.screen.notifications.NotificationListScreen
import com.android.rut.miit.productinventory.ui.screen.products.AddProductScreen
import com.android.rut.miit.productinventory.ui.screen.products.ProductListScreen
import com.android.rut.miit.productinventory.ui.screen.profile.ProfileScreen
import com.android.rut.miit.productinventory.ui.screen.barcode.BarcodeScannerScreen
import com.android.rut.miit.productinventory.ui.screen.recipes.RecipeListScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = Route.Login) {
            composable<Route.Login> {
                LoginScreen(
                    onNavigateToHome = {
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
                    onBack = { navController.popBackStack() },
                    onNavigateToRecipes = { navController.navigate(Route.Recipes(route.householdId)) },
                    onNavigateToNotifications = { navController.navigate(Route.Notifications) },
                    onNavigateToBarcodeScan = { navController.navigate(Route.BarcodeScan(route.householdId)) }
                )
            }

            composable<Route.AddProduct> { entry ->
                val route = entry.toRoute<Route.AddProduct>()
                AddProductScreen(
                    householdId = route.householdId,
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

            composable<Route.Notifications> {
                NotificationListScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.Profile> {
                ProfileScreen(
                    onNavigateToLogin = {
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
                        navController.navigate(Route.AddProduct(route.householdId))
                    }
                )
            }
        }
    }
}
