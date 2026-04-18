package com.herdroid.app.ui.main

enum class ChatMessageRole {
    User,
    Assistant,
}

data class ChatUiMessage(
    val id: Long,
    val role: ChatMessageRole,
    val text: String,
)
