package com.herdroid.app.domain

/**
 * Minimal cleanup before TTS; extend when HA message format (JSON/Markdown) is fixed.
 */
object MessageSanitizer {
    fun forSpeech(raw: String): String {
        return raw
            .replace(Regex("\\*+|`+|#+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
