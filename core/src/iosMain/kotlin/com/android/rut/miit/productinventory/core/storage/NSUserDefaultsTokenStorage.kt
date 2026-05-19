package com.android.rut.miit.productinventory.core.storage

import platform.Foundation.NSUserDefaults

class NSUserDefaultsTokenStorage : TokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val ACCESS_TOKEN_KEY = "access_token"
    private val REFRESH_TOKEN_KEY = "refresh_token"
    private val USER_ID_KEY = "user_id"

    override fun getAccessToken(): String? = defaults.stringForKey(ACCESS_TOKEN_KEY)
    override fun getRefreshToken(): String? = defaults.stringForKey(REFRESH_TOKEN_KEY)

    override fun saveTokens(accessToken: String, refreshToken: String) {
        defaults.setObject(accessToken, ACCESS_TOKEN_KEY)
        defaults.setObject(refreshToken, REFRESH_TOKEN_KEY)
    }

    override fun clearTokens() {
        defaults.removeObjectForKey(ACCESS_TOKEN_KEY)
        defaults.removeObjectForKey(REFRESH_TOKEN_KEY)
        defaults.removeObjectForKey(USER_ID_KEY)
    }

    override fun getUserId(): String? = defaults.stringForKey(USER_ID_KEY)

    override fun saveUserId(userId: String) {
        defaults.setObject(userId, USER_ID_KEY)
    }
}
