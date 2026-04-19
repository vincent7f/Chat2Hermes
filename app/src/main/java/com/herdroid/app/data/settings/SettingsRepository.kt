package com.herdroid.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "herdroid_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SCHEME = stringPreferencesKey("scheme")
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val API_KEY = stringPreferencesKey("network_tts_api_key")
        val MODEL_NAME = stringPreferencesKey("network_tts_model")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        p.toUserPreferences()
    }

    suspend fun update(
        scheme: String,
        host: String,
        port: Int,
        apiKey: String,
        modelName: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCHEME] = scheme.trim()
            prefs[Keys.HOST] = host.trim()
            prefs[Keys.PORT] = port
            prefs[Keys.API_KEY] = apiKey.trim()
            prefs[Keys.MODEL_NAME] = modelName.trim()
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        return UserPreferences(
            scheme = this[Keys.SCHEME] ?: "http",
            host = this[Keys.HOST] ?: "192.168.3.112",
            port = this[Keys.PORT] ?: 8642,
            apiKey = this[Keys.API_KEY] ?: "myapiky",
            modelName = this[Keys.MODEL_NAME] ?: "hermes-agent",
        )
    }
}
