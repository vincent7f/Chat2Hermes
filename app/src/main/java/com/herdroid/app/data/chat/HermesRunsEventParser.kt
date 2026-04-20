package com.herdroid.app.data.chat

/** Runs API 的轻量 JSON/SSE 解析，兼容 chat-completions 与 responses 风格事件。 */
object HermesRunsEventParser {

    fun extractRunId(rawJson: String): String? {
        if (rawJson.isBlank()) return null
        return extractJsonString(rawJson, "run_id")
            ?: extractJsonString(rawJson, "id")
    }

    fun extractTextDelta(eventName: String?, rawData: String): String? {
        if (rawData.isBlank() || rawData == "[DONE]") return null
        @Suppress("UNUSED_VARIABLE")
        val ignoredEventName = eventName
        return extractJsonString(rawData, "content")
            ?: extractJsonString(rawData, "delta")
            ?: extractJsonString(rawData, "text")
    }

    fun isTerminalEvent(eventName: String?, rawData: String): Boolean {
        if (rawData == "[DONE]") return true
        val e = eventName.orEmpty().lowercase()
        if (e == "response.completed" || e == "run.completed" || e == "completed") return true
        if (e == "run.failed" || e == "response.failed" || e == "error") return true
        val status = extractJsonString(rawData, "status")?.lowercase()
        return status == "completed" || status == "failed" || status == "cancelled"
    }

    private fun extractJsonString(raw: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = regex.find(raw) ?: return null
        return unescapeJsonString(match.groupValues[1]).takeIf { it.isNotEmpty() }
    }

    private fun unescapeJsonString(v: String): String {
        return v
            .replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
