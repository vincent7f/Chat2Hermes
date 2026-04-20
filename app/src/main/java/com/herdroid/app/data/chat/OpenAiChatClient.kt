package com.herdroid.app.data.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * OpenAI 兼容 Chat Completions（[POST /v1/chat/completions](https://platform.openai.com/docs/api-reference/chat/create)）。
 *
 * 对接 Hermes Agent API Server 时使用 **`stream: true`**，按 **SSE**（`text/event-stream`）解析
 * `chat.completion.chunk`；Hermes 自定义的 `event: hermes.tool.progress` 等非 `choices` 行会被跳过。
 * 详见项目内 `docs/HERMES_API_SERVER.md`；官方文档：
 * [API Server | Hermes Agent](https://hermes-agent.nousresearch.com/docs/user-guide/features/api-server?sharetype=wechat)。
 */
class OpenAiChatClient(private val httpClient: OkHttpClient) {

    /**
     * @param messages 有序列表，每项为 role（user/assistant/system）与 content。
     * @param onContentDelta 每收到一段 `delta.content` 时回调（在 OkHttp 读线程上调用，勿直接更新 Compose；由调用方投递主线程）。
     * @return 拼接后的完整助手文本；流为空或仅含非文本事件时返回 failure。
     */
    fun chatCompletionsStream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        return try {
            val root = baseUrl.trim().trimEnd('/')
            if (root.isEmpty()) return Result.failure(IllegalArgumentException("empty base url"))
            if (messages.isEmpty()) return Result.failure(IllegalArgumentException("empty messages"))

            val url = "$root/v1/chat/completions"
            val body = JSONObject()
            body.put("model", model.trim().ifEmpty { "hermes-agent" })
            body.put("stream", true)
            val arr = JSONArray()
            for ((role, content) in messages) {
                arr.put(
                    JSONObject().put("role", role).put("content", content),
                )
            }
            body.put("messages", arr)

            val reqBody = body.toString().toRequestBody(JSON_MEDIA)
            val builder = Request.Builder()
                .url(url)
                .post(reqBody)
                .header("Accept", "text/event-stream")
            val key = apiKey.trim()
            if (key.isNotEmpty()) {
                builder.header("Authorization", "Bearer $key")
            }

            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = resp.body?.string().orEmpty()
                    val detail = parseErrorMessage(errBody) ?: errBody.ifEmpty { "HTTP ${resp.code}" }
                    return Result.failure(RuntimeException(detail))
                }
                readChatCompletion(resp, onContentDelta)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readChatCompletion(
        response: Response,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        val ct = response.header("Content-Type").orEmpty().lowercase()
        return if (ct.contains("text/event-stream")) {
            readChatCompletionSse(response, onContentDelta)
        } else {
            readChatCompletionJson(response, onContentDelta)
        }
    }

    private fun readChatCompletionSse(
        response: Response,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        val body = response.body ?: return Result.failure(RuntimeException("empty body"))
        val acc = StringBuilder()
        body.byteStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                val trimmed = line.trimEnd('\r')
                if (trimmed.isEmpty()) continue
                if (trimmed.startsWith(":")) continue
                if (!trimmed.startsWith("data:")) continue
                val raw = trimmed.substring(5).trim()
                if (raw == "[DONE]") break
                when (val chunk = parseSseJsonLine(raw)) {
                    is SseChunk.Content -> {
                        acc.append(chunk.text)
                        onContentDelta(chunk.text)
                    }
                    is SseChunk.Error -> return Result.failure(RuntimeException(chunk.message))
                    SseChunk.Skip -> Unit
                }
            }
        }
        val full = acc.toString()
        if (full.isEmpty()) {
            return Result.failure(RuntimeException("empty response stream"))
        }
        return Result.success(full)
    }

    private fun readChatCompletionJson(
        response: Response,
        onContentDelta: (String) -> Unit,
    ): Result<String> {
        val raw = response.body?.string().orEmpty()
        if (raw.isEmpty()) return Result.failure(RuntimeException("empty body"))
        return try {
            val root = JSONObject(raw)
            root.optJSONObject("error")?.let { err ->
                val msg = err.optString("message").ifEmpty { err.toString() }
                return Result.failure(RuntimeException(msg))
            }
            val choices = root.optJSONArray("choices")
                ?: return Result.failure(RuntimeException("missing choices"))
            val first = choices.optJSONObject(0)
                ?: return Result.failure(RuntimeException("missing first choice"))
            val text = first
                .optJSONObject("message")
                ?.optString("content", "")
                .orEmpty()
                .ifEmpty {
                    first.optJSONObject("delta")?.optString("content", "").orEmpty()
                }
            if (text.isEmpty()) {
                return Result.failure(RuntimeException("empty completion content"))
            }
            onContentDelta(text)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSseJsonLine(raw: String): SseChunk {
        return try {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.let { err ->
                val msg = err.optString("message").ifEmpty { err.toString() }
                return SseChunk.Error(msg)
            }
            val choices = json.optJSONArray("choices") ?: return SseChunk.Skip
            val first = choices.optJSONObject(0) ?: return SseChunk.Skip
            val delta = first.optJSONObject("delta") ?: return SseChunk.Skip
            val piece = delta.optString("content", "")
            if (piece.isEmpty()) SseChunk.Skip else SseChunk.Content(piece)
        } catch (_: Exception) {
            SseChunk.Skip
        }
    }

    private sealed class SseChunk {
        data class Content(val text: String) : SseChunk()
        data class Error(val message: String) : SseChunk()
        data object Skip : SseChunk()
    }

    private fun parseErrorMessage(body: String): String? {
        if (body.isEmpty()) return null
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
