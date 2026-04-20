package com.herdroid.app.data.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
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
        maxReconnectAttempts: Int,
        onReconnect: (attempt: Int) -> Unit = {},
    ): Result<String> {
        val runId = createRun(baseUrl, apiKey, model, messages).getOrElse { return Result.failure(it) }
        return continueRunAndCollectText(
            baseUrl = baseUrl,
            apiKey = apiKey,
            runId = runId,
            onContentDelta = onContentDelta,
            maxReconnectAttempts = maxReconnectAttempts,
            onReconnect = onReconnect,
        )
    }

    fun continueRunAndCollectText(
        baseUrl: String,
        apiKey: String,
        runId: String,
        onContentDelta: (String) -> Unit,
        maxReconnectAttempts: Int,
        onReconnect: (attempt: Int) -> Unit = {},
    ): Result<String> {
        val full = StringBuilder()
        var reconnectAttempt = 0
        val maxAttempts = maxReconnectAttempts.coerceIn(0, 10)
        while (true) {
            var catchUpCursor = if (reconnectAttempt > 0) 0 else full.length
            val result = streamRunEvents(baseUrl, apiKey, runId) { delta ->
                val append = RunsDeltaDeduper.mergeDelta(
                    accumulated = full,
                    incoming = delta,
                    catchUpCursor = catchUpCursor,
                )
                catchUpCursor = append.nextCatchUpCursor
                if (append.emittable.isNotEmpty()) {
                    onContentDelta(append.emittable)
                }
            }
            if (result.isSuccess) {
                if (full.isEmpty()) return Result.failure(RuntimeException("empty run output"))
                return Result.success(full.toString())
            }
            val failure = result.exceptionOrNull() ?: RuntimeException("unknown runs stream error")
            val canRetry = reconnectAttempt < maxAttempts && isRecoverableStreamError(failure)
            if (!canRetry) return Result.failure(failure)
            reconnectAttempt += 1
            onReconnect(reconnectAttempt)
            Thread.sleep(RECONNECT_BACKOFF_MS)
        }
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
        private const val RECONNECT_BACKOFF_MS = 1000L
    }

    private fun isRecoverableStreamError(t: Throwable): Boolean {
        if (t is IOException) return true
        val msg = t.message.orEmpty().lowercase()
        return msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("connection reset") ||
            msg.contains("stream was reset")
    }
}

/** 断线重连后去重：若服务端从头回放，先追平历史，再仅向 UI 发新增增量。 */
internal object RunsDeltaDeduper {
    data class MergeResult(
        val emittable: String,
        val nextCatchUpCursor: Int,
    )

    fun mergeDelta(
        accumulated: StringBuilder,
        incoming: String,
        catchUpCursor: Int,
    ): MergeResult {
        if (incoming.isEmpty()) return MergeResult("", catchUpCursor)
        if (catchUpCursor >= accumulated.length) {
            accumulated.append(incoming)
            return MergeResult(incoming, catchUpCursor + incoming.length)
        }
        val remain = accumulated.substring(catchUpCursor)
        if (remain.startsWith(incoming)) {
            return MergeResult("", catchUpCursor + incoming.length)
        }
        val overlap = commonPrefixLen(remain, incoming)
        val newPart = incoming.substring(overlap)
        if (newPart.isNotEmpty()) {
            accumulated.append(newPart)
        }
        return MergeResult(newPart, catchUpCursor + overlap + newPart.length)
    }

    private fun commonPrefixLen(a: String, b: String): Int {
        val n = minOf(a.length, b.length)
        var i = 0
        while (i < n && a[i] == b[i]) i += 1
        return i
    }
}
