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

    /**
     * 与 [forSpeech] 相同清洗规则，但**保留换行**，供歌词式按行分段朗读/UI 使用。
     * （[forSpeech] 会将 `\s+` 压成空格，会破坏段落结构。）
     */
    fun forSpeechPreserveParagraphs(raw: String): String {
        return raw.split(Regex("\r?\n"))
            .map { line ->
                line
                    .replace(Regex("`+"), " ")
                    .replace(Regex("\\*+"), " ")
                    .replace(Regex("#+\\s*"), " ")
                    .replace(Regex("""\[([^\]]+)]\([^)]+\)""")) { m -> m.groupValues[1] }
                    .replace(Regex("[ \\t]+"), " ")
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}
