package com.android.rut.miit.productinventory.core.storage

class InMemoryTokenStorage : TokenStorage {
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
