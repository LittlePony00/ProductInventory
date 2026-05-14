package com.android.rut.miit.productinventory.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object Register : Route
    @Serializable data object HouseholdList : Route
    @Serializable data class ProductList(val householdId: String) : Route
    @Serializable data class AddProduct(val householdId: String) : Route
    @Serializable data class Recipes(val householdId: String) : Route
    @Serializable data object Notifications : Route
    @Serializable data object Profile : Route
    @Serializable data class BarcodeScan(val householdId: String) : Route
}
