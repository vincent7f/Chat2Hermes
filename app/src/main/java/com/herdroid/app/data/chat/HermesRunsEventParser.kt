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
        val out = StringBuilder(v.length)
        var i = 0
        while (i < v.length) {
            val ch = v[i]
            if (ch != '\\' || i + 1 >= v.length) {
                out.append(ch)
                i += 1
                continue
            }
            val esc = v[i + 1]
            when (esc) {
                '\\' -> {
                    out.append('\\')
                    i += 2
                }
                '"' -> {
                    out.append('"')
                    i += 2
                }
                'n' -> {
                    out.append('\n')
                    i += 2
                }
                'r' -> {
                    out.append('\r')
                    i += 2
                }
                't' -> {
                    out.append('\t')
                    i += 2
                }
                'u' -> {
                    if (i + 6 <= v.length) {
                        val hex = v.substring(i + 2, i + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                            i += 6
                        } else {
                            out.append("\\u")
                            i += 2
                        }
                    } else {
                        out.append("\\u")
                        i += 2
                    }
                }
                else -> {
                    // 未识别转义：按原文保留
                    out.append('\\').append(esc)
                    i += 2
                }
            }
        }
        return out.toString()
    }
}
