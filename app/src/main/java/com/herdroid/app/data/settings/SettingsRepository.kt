package com.herdroid.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val AUTO_PLAY_TTS = booleanPreferencesKey("auto_play_tts")
        val TTS_ENGINE = stringPreferencesKey("tts_engine")
        val NETWORK_TTS_BASE = stringPreferencesKey("network_tts_base_url")
        val NETWORK_TTS_API_KEY = stringPreferencesKey("network_tts_api_key")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        p.toUserPreferences()
    }

    suspend fun update(
        scheme: String,
        host: String,
        port: Int,
        autoPlayTts: Boolean,
        ttsEngine: TtsEngineType,
        networkTtsBaseUrl: String,
        networkTtsApiKey: String,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCHEME] = scheme.trim()
            prefs[Keys.HOST] = host.trim()
            prefs[Keys.PORT] = port
            prefs[Keys.AUTO_PLAY_TTS] = autoPlayTts
            prefs[Keys.TTS_ENGINE] = ttsEngine.name
            prefs[Keys.NETWORK_TTS_BASE] = networkTtsBaseUrl.trim()
            prefs[Keys.NETWORK_TTS_API_KEY] = networkTtsApiKey.trim()
        }
    }

    suspend fun setAutoPlayTts(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_TTS] = enabled }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val engineName = this[Keys.TTS_ENGINE] ?: TtsEngineType.SYSTEM.name
        val engine = runCatching { TtsEngineType.valueOf(engineName) }
            .getOrDefault(TtsEngineType.SYSTEM)
        return UserPreferences(
            scheme = this[Keys.SCHEME] ?: "ws",
            host = this[Keys.HOST] ?: "",
            port = this[Keys.PORT] ?: 8080,
            autoPlayTts = this[Keys.AUTO_PLAY_TTS] ?: false,
            ttsEngine = engine,
            networkTtsBaseUrl = this[Keys.NETWORK_TTS_BASE] ?: "",
            networkTtsApiKey = this[Keys.NETWORK_TTS_API_KEY] ?: "",
        )
    }
}
