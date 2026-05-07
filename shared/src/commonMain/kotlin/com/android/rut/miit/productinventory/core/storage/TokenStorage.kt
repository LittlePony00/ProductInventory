package com.android.rut.miit.productinventory.core.storage

interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String)
    fun clearTokens()
    fun getUserId(): String?
    fun saveUserId(userId: String)
}
