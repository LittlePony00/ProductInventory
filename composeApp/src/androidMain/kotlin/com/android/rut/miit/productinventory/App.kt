package com.android.rut.miit.productinventory

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.android.rut.miit.productinventory.ui.screen.auth.LoginScreen
import com.android.rut.miit.productinventory.ui.screen.auth.RegisterScreen
import com.android.rut.miit.productinventory.ui.screen.household.HouseholdListScreen
import com.android.rut.miit.productinventory.ui.screen.notifications.NotificationListScreen
import com.android.rut.miit.productinventory.ui.screen.products.AddProductScreen
import com.android.rut.miit.productinventory.ui.screen.products.ProductListScreen
import com.android.rut.miit.productinventory.ui.screen.profile.ProfileScreen
import com.android.rut.miit.productinventory.ui.screen.recipes.RecipeListScreen

sealed class Screen {
    data object Login : Screen()
    data object Register : Screen()
    data object HouseholdList : Screen()
    data class ProductList(val householdId: String) : Screen()
    data class AddProduct(val householdId: String) : Screen()
    data class Recipes(val householdId: String) : Screen()
    data object Notifications : Screen()
    data object Profile : Screen()
}

@Composable
fun App() {
    MaterialTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Login) }
        val currentScreen = backStack.last()

        fun navigate(screen: Screen) { backStack.add(screen) }
        fun goBack() { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
        fun navigateAndClear(screen: Screen) { backStack.clear(); backStack.add(screen) }

        when (currentScreen) {
            is Screen.Login -> LoginScreen(
                onNavigateToHome = { navigateAndClear(Screen.HouseholdList) },
                onNavigateToRegister = { navigate(Screen.Register) }
            )
            is Screen.Register -> RegisterScreen(
                onNavigateToHome = { navigateAndClear(Screen.HouseholdList) },
                onNavigateBack = { goBack() }
            )
            is Screen.HouseholdList -> HouseholdListScreen(
                onNavigateToHousehold = { id -> navigate(Screen.ProductList(id)) },
                onNavigateToProfile = { navigate(Screen.Profile) }
            )
            is Screen.ProductList -> ProductListScreen(
                householdId = currentScreen.householdId,
                onAddProduct = { navigate(Screen.AddProduct(currentScreen.householdId)) },
                onBack = { goBack() },
                onNavigateToRecipes = { navigate(Screen.Recipes(currentScreen.householdId)) },
                onNavigateToNotifications = { navigate(Screen.Notifications) }
            )
            is Screen.AddProduct -> AddProductScreen(
                householdId = currentScreen.householdId,
                onProductAdded = { goBack() },
                onBack = { goBack() }
            )
            is Screen.Recipes -> RecipeListScreen(
                householdId = currentScreen.householdId,
                onBack = { goBack() }
            )
            is Screen.Notifications -> NotificationListScreen(
                onBack = { goBack() }
            )
            is Screen.Profile -> ProfileScreen(
                onNavigateToLogin = { navigateAndClear(Screen.Login) },
                onBack = { goBack() }
            )
        }
    }
}
