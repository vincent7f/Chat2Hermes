package com.herdroid.app.data.chat

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatSessionStore by preferencesDataStore(name = "herdroid_chat_session")

/**
 * 本地持久化当前对话的 session id 与消息列表（用于下次启动「继续上次对话」）。
 * 与 Hermes 无状态 HTTP 无关；session id 为客户端生成的对话标识。
 */
class ChatSessionRepository(context: Context) {

    private val app = context.applicationContext

    private object Keys {
        val SESSION_ID = stringPreferencesKey("conversation_session_id")
        val MESSAGES_JSON = stringPreferencesKey("conversation_messages_json")
    }

    val sessionIdFlow: Flow<String?> = app.chatSessionStore.data.map { it[Keys.SESSION_ID] }

    val messagesJsonFlow: Flow<String?> = app.chatSessionStore.data.map { it[Keys.MESSAGES_JSON] }

    suspend fun persistConversation(sessionId: String, messagesJson: String) {
        app.chatSessionStore.edit { prefs ->
            prefs[Keys.SESSION_ID] = sessionId
            prefs[Keys.MESSAGES_JSON] = messagesJson
        }
    }

    suspend fun clearPersistedConversation() {
        app.chatSessionStore.edit { prefs ->
            prefs.remove(Keys.SESSION_ID)
            prefs.remove(Keys.MESSAGES_JSON)
        }
    }
}
