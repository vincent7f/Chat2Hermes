package com.herdroid.app.ui.main

enum class ChatMessageRole {
    User,
    Assistant,
}

/** 仅 [ChatMessageRole.User] 使用：从发出到收到接口结果的状态。 */
enum class UserMessageSendState {
    Sending,
    Sent,
    Failed,
}

data class ChatUiMessage(
    val id: Long,
    val role: ChatMessageRole,
    val text: String,
    /** 用户消息的发送状态；助手消息为 `null`。 */
    val userSendState: UserMessageSendState? = null,
)
