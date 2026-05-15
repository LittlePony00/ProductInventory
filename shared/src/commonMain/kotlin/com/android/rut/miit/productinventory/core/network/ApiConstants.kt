package com.android.rut.miit.productinventory.core.network

object ApiConstants {
    val BASE_URL: String = apiBaseUrl
    val API_V1: String = "$BASE_URL/api/v1"
}

expect val apiBaseUrl: String
