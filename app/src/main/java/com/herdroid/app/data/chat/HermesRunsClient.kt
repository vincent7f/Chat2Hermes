package com.herdroid.app.data.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Hermes Runs API：
 * 1) POST /v1/runs 创建 run
 * 2) GET /v1/runs/{run_id}/events 订阅 SSE 事件
 */
class HermesRunsClient(private val httpClient: OkHttpClient) {

    fun createRun(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>,
    ): Result<String> {
        return try {
            val root = baseUrl.trim().trimEnd('/')
            if (root.isEmpty()) return Result.failure(IllegalArgumentException("empty base url"))
            if (messages.isEmpty()) return Result.failure(IllegalArgumentException("empty messages"))

            val body = JSONObject().apply {
                put("model", model.trim().ifEmpty { "hermes-agent" })
                put("messages", JSONArray().apply {
                    messages.forEach { (role, content) ->
                        put(JSONObject().put("role", role).put("content", content))
                    }
                })
            }
            val req = authorizedBuilder("$root/v1/runs", apiKey)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    val detail = parseErrorMessage(err) ?: err.ifEmpty { "HTTP ${resp.code}" }
                    return Result.failure(RuntimeException(detail))
                }
                val runId = HermesRunsEventParser.extractRunId(resp.body?.string().orEmpty())
                    ?: return Result.failure(RuntimeException("missing run_id"))
                Result.success(runId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamRunEvents(
        baseUrl: String,
        apiKey: String,
        runId: String,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        return try {
            val root = baseUrl.trim().trimEnd('/')
            if (root.isEmpty()) return Result.failure(IllegalArgumentException("empty base url"))
            if (runId.isBlank()) return Result.failure(IllegalArgumentException("empty run id"))
            val req = authorizedBuilder("$root/v1/runs/${runId.trim()}/events", apiKey)
                .get()
                .header("Accept", "text/event-stream")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    val detail = parseErrorMessage(err) ?: err.ifEmpty { "HTTP ${resp.code}" }
                    return Result.failure(RuntimeException(detail))
                }
                readEventStream(resp.body?.byteStream()?.bufferedReader(StandardCharsets.UTF_8), onContentDelta)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun runAndCollectText(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        val runId = createRun(baseUrl, apiKey, model, messages).getOrElse { return Result.failure(it) }
        return streamRunEvents(baseUrl, apiKey, runId, onContentDelta)
    }

    private fun readEventStream(
        reader: java.io.BufferedReader?,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        if (reader == null) return Result.failure(RuntimeException("empty body"))
        val acc = StringBuilder()
        var currentEvent: String? = null
        val dataLines = ArrayList<String>()

        fun flushEvent() {
            if (dataLines.isEmpty()) {
                currentEvent = null
                return
            }
            val payload = dataLines.joinToString("\n")
            HermesRunsEventParser.extractTextDelta(currentEvent, payload)?.let {
                acc.append(it)
                onContentDelta(it)
            }
            currentEvent = null
            dataLines.clear()
        }

        reader.use { r ->
            while (true) {
                val line = r.readLine() ?: break
                val trimmed = line.trimEnd('\r')
                if (trimmed.isEmpty()) {
                    flushEvent()
                    continue
                }
                if (trimmed.startsWith(":")) continue
                if (trimmed.startsWith("event:")) {
                    currentEvent = trimmed.substringAfter("event:").trim()
                    continue
                }
                if (trimmed.startsWith("data:")) {
                    val data = trimmed.substringAfter("data:").trimStart()
                    dataLines.add(data)
                    if (HermesRunsEventParser.isTerminalEvent(currentEvent, data)) {
                        flushEvent()
                        break
                    }
                }
            }
            flushEvent()
        }
        val full = acc.toString()
        if (full.isEmpty()) return Result.failure(RuntimeException("empty run output"))
        return Result.success(full)
    }

    private fun authorizedBuilder(url: String, apiKey: String): Request.Builder {
        val b = Request.Builder().url(url)
        val key = apiKey.trim()
        if (key.isNotEmpty()) b.header("Authorization", "Bearer $key")
        return b
    }

    private fun parseErrorMessage(body: String): String? {
        if (body.isEmpty()) return null
        return runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
