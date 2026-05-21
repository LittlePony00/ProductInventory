package com.android.rut.miit.productinventory.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object AuthBootstrap : Route
    @Serializable data object Login : Route
    @Serializable data object Register : Route
    @Serializable data object HouseholdList : Route
    @Serializable data class ProductList(val householdId: String) : Route
    @Serializable
    data class AddProduct(
        val householdId: String,
        val productId: String? = null,
        val barcode: String? = null,
        val name: String? = null,
        val brand: String? = null,
        val category: String? = null,
        val packageAmount: String? = null,
        val packageUnit: String? = null,
        val ingredientsText: String? = null,
        val imageUrl: String? = null,
        val localImagePath: String? = null,
        val calories: String? = null,
        val protein: String? = null,
        val fat: String? = null,
        val carbs: String? = null
    ) : Route
    @Serializable data class Recipes(val householdId: String) : Route
    @Serializable data class Categories(val householdId: String) : Route
    @Serializable data object Notifications : Route
    @Serializable data class Profile(val householdId: String? = null) : Route
    @Serializable data class BarcodeScan(val householdId: String) : Route
}
