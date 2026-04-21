package com.herdroid.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "herdroid_settings")

/**
 * 设置按 **profile** 分文件式键隔离：`p_<profileId>_…`，当前 profile 见 [Meta.ACTIVE]。
 * 首次启动将旧版扁平键迁移到 `default` profile。
 */
class SettingsRepository(private val context: Context) {

    private object Legacy {
        val SCHEME = stringPreferencesKey("scheme")
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
        val API_KEY = stringPreferencesKey("network_tts_api_key")
        val MODEL_NAME = stringPreferencesKey("network_tts_model")
        val AUTO_PLAY_TTS = booleanPreferencesKey("auto_play_tts")
    }

    private object Meta {
        val ACTIVE = stringPreferencesKey("meta_active_profile")
        val PROFILES_CSV = stringPreferencesKey("meta_profiles_csv")
        val MIGRATED = booleanPreferencesKey("meta_profiles_storage_v1")
    }

    private fun kStr(profileId: String, suffix: String) =
        stringPreferencesKey("p_${profileId}_$suffix")

    private fun kInt(profileId: String, suffix: String) =
        intPreferencesKey("p_${profileId}_$suffix")

    private fun kBool(profileId: String, suffix: String) =
        booleanPreferencesKey("p_${profileId}_$suffix")

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val active = prefs[Meta.ACTIVE] ?: "default"
        prefs.toUserPreferences(active)
    }

    val activeProfileId: Flow<String> = context.dataStore.data.map { it[Meta.ACTIVE] ?: "default" }

    val profileIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        (prefs[Meta.PROFILES_CSV] ?: "default")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * 将旧版（无 profile 前缀）键迁移到 `default` profile；应在进程早期调用一次。
     */
    suspend fun migrateFromLegacyIfNeeded() {
        context.dataStore.edit { prefs ->
            if (prefs[Meta.MIGRATED] == true) return@edit
            val def = "default"
            val hasLegacy =
                prefs[Legacy.HOST] != null ||
                    prefs[Legacy.SCHEME] != null ||
                    prefs[Legacy.PORT] != null
            if (hasLegacy) {
                prefs[kStr(def, "scheme")] = prefs[Legacy.SCHEME] ?: "http"
                prefs[kStr(def, "host")] = prefs[Legacy.HOST] ?: UserPreferences.DEFAULT.host
                prefs[kInt(def, "port")] = prefs[Legacy.PORT] ?: UserPreferences.DEFAULT.port
                prefs[kStr(def, "api_key")] = prefs[Legacy.API_KEY] ?: UserPreferences.DEFAULT.apiKey
                prefs[kStr(def, "model_name")] = prefs[Legacy.MODEL_NAME] ?: UserPreferences.DEFAULT.modelName
                prefs[kBool(def, "auto_play_tts")] = prefs[Legacy.AUTO_PLAY_TTS] ?: false
                prefs[kInt(def, "runs_auto_reconnect_attempts")] =
                    UserPreferences.DEFAULT.runsAutoReconnectAttempts
            } else {
                seedDefaultProfile(prefs, def)
            }
            prefs[Meta.PROFILES_CSV] = def
            prefs[Meta.ACTIVE] = def
            prefs[Meta.MIGRATED] = true
        }
    }

    private fun seedDefaultProfile(prefs: MutablePreferences, profileId: String) {
        val d = UserPreferences.DEFAULT
        prefs[kStr(profileId, "scheme")] = d.scheme
        prefs[kStr(profileId, "host")] = d.host
        prefs[kInt(profileId, "port")] = d.port
        prefs[kStr(profileId, "api_key")] = d.apiKey
        prefs[kStr(profileId, "model_name")] = d.modelName
        prefs[kBool(profileId, "auto_play_tts")] = d.autoPlayTts
        prefs[kInt(profileId, "runs_auto_reconnect_attempts")] = d.runsAutoReconnectAttempts
    }

    suspend fun update(
        scheme: String,
        host: String,
        port: Int,
        apiKey: String,
        modelName: String,
        runsAutoReconnectAttempts: Int,
    ) {
        val active = context.dataStore.data.first()[Meta.ACTIVE] ?: "default"
        context.dataStore.edit { prefs ->
            prefs[kStr(active, "scheme")] = scheme.trim()
            prefs[kStr(active, "host")] = host.trim()
            prefs[kInt(active, "port")] = port
            prefs[kStr(active, "api_key")] = apiKey.trim()
            prefs[kStr(active, "model_name")] = modelName.trim()
            prefs[kInt(active, "runs_auto_reconnect_attempts")] =
                runsAutoReconnectAttempts.coerceIn(0, 10)
        }
    }

    suspend fun setAutoPlayTts(enabled: Boolean) {
        val active = context.dataStore.data.first()[Meta.ACTIVE] ?: "default"
        context.dataStore.edit { prefs ->
            prefs[kBool(active, "auto_play_tts")] = enabled
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        val ids = context.dataStore.data.first()[Meta.PROFILES_CSV] ?: "default"
        val set = ids.split(',').map { it.trim() }.toSet()
        require(profileId in set) { "unknown profile: $profileId" }
        context.dataStore.edit { it[Meta.ACTIVE] = profileId }
    }

    /** 读取指定 profile 的即时快照（不依赖当前 active profile）。 */
    suspend fun getPreferencesSnapshotForProfile(profileId: String): UserPreferences {
        val pid = profileId.ifBlank { "default" }
        return context.dataStore.data.first().toUserPreferences(pid)
    }

    /**
     * @return 新 profile 的 id，若重名或非法则返回 `null`。
     */
    suspend fun addProfile(displayName: String): String? {
        val raw = displayName.trim()
        if (raw.isEmpty()) return null
        val sanitized = raw
            .lowercase()
            .replace(Regex("[^a-z0-9._-]"), "_")
            .trim('_', '.')
            .take(32)
        if (sanitized.isEmpty()) return null
        val current = preferencesFlow.first()
        var newId = sanitized
        val existing = context.dataStore.data.first()[Meta.PROFILES_CSV] ?: "default"
        val list = existing.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        var n = 2
        while (newId in list) {
            newId = "${sanitized}_$n"
            n++
        }
        context.dataStore.edit { prefs ->
            list.add(newId)
            prefs[Meta.PROFILES_CSV] = list.joinToString(",")
            prefs[kStr(newId, "scheme")] = current.scheme
            prefs[kStr(newId, "host")] = current.host
            prefs[kInt(newId, "port")] = current.port
            prefs[kStr(newId, "api_key")] = current.apiKey
            prefs[kStr(newId, "model_name")] = current.modelName
            prefs[kBool(newId, "auto_play_tts")] = current.autoPlayTts
            prefs[kInt(newId, "runs_auto_reconnect_attempts")] = current.runsAutoReconnectAttempts
        }
        return newId
    }

    private fun Preferences.toUserPreferences(profileId: String): UserPreferences {
        val pid = profileId.ifEmpty { "default" }
        return UserPreferences(
            scheme = this[kStr(pid, "scheme")] ?: "http",
            host = this[kStr(pid, "host")] ?: UserPreferences.DEFAULT.host,
            port = this[kInt(pid, "port")] ?: UserPreferences.DEFAULT.port,
            apiKey = this[kStr(pid, "api_key")] ?: UserPreferences.DEFAULT.apiKey,
            modelName = this[kStr(pid, "model_name")] ?: UserPreferences.DEFAULT.modelName,
            autoPlayTts = this[kBool(pid, "auto_play_tts")] ?: false,
            runsAutoReconnectAttempts = this[kInt(pid, "runs_auto_reconnect_attempts")]
                ?: UserPreferences.DEFAULT.runsAutoReconnectAttempts,
        )
    }
}
