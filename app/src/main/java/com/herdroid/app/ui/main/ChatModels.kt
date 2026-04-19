package com.herdroid.app.ui.main

/** 折叠行展示：取正文开头若干字符 + 省略号，与收发消息共用。 */
object ChatUiStyle {
    const val COLLAPSED_PREFIX_CHARS = 12
}

/** 单行折叠预览：至多 [maxChars] 个字符，超出加 `…`。 */
fun collapsedPrefixPreview(text: String, maxChars: Int = ChatUiStyle.COLLAPSED_PREFIX_CHARS): String {
    val t = text.trim()
    if (t.isEmpty()) return ""
    return if (t.length <= maxChars) t else t.take(maxChars) + "…"
}

/** 与上一条消息间隔不小于此时长（毫秒）才单独显示时间行。 */
const val CHAT_TIMESTAMP_GAP_MS = 60_000L

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
    /**
     * 助手消息：SSE 是否已结束（可折叠展示）；用户消息忽略，恒为 `true`。
     */
    val replyComplete: Boolean = true,
    /** 本条消息用于时间戳显示的本地时间（毫秒）。 */
    val timeMillis: Long,
)
