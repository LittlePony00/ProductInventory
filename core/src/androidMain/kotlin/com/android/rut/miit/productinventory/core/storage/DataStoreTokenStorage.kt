package com.android.rut.miit.productinventory.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class DataStoreTokenStorage(private val context: Context) : TokenStorage {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
    }

    override fun getAccessToken(): String? = runBlocking {
        context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }.first()
    }

    override fun getRefreshToken(): String? = runBlocking {
        context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        runBlocking {
            context.dataStore.edit {
                it[Keys.ACCESS_TOKEN] = accessToken
                it[Keys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    override fun clearTokens() {
        runBlocking {
            context.dataStore.edit {
                it.remove(Keys.ACCESS_TOKEN)
                it.remove(Keys.REFRESH_TOKEN)
                it.remove(Keys.USER_ID)
            }
        }
    }

    override fun getUserId(): String? = runBlocking {
        context.dataStore.data.map { it[Keys.USER_ID] }.first()
    }

    override fun saveUserId(userId: String) {
        runBlocking {
            context.dataStore.edit { it[Keys.USER_ID] = userId }
        }
    }
}
