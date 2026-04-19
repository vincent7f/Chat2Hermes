package com.herdroid.app.domain

/** 供 TTS 使用的纯文本：去掉常见 Markdown/符号，避免朗读无意义字符。 */
object MessageSanitizer {

    fun forSpeech(raw: String): String {
        return raw
            .replace(Regex("`+"), " ")
            .replace(Regex("\\*+"), " ")
            .replace(Regex("#+\\s*"), " ")
            .replace(Regex("""\[([^\]]+)]\([^)]+\)""")) { m -> m.groupValues[1] }
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
