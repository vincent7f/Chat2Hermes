package com.herdroid.app.data.chat

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI 兼容 Chat Completions（[POST /v1/chat/completions](https://platform.openai.com/docs/api-reference/chat/create)）。
 */
class OpenAiChatClient(private val httpClient: OkHttpClient) {

    /**
     * @param messages 有序列表，每项为 role（user/assistant/system）与 content。
     */
    fun chatCompletions(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<Pair<String, String>>,
    ): Result<String> {
        return try {
            val root = baseUrl.trim().trimEnd('/')
            if (root.isEmpty()) return Result.failure(IllegalArgumentException("empty base url"))
            if (messages.isEmpty()) return Result.failure(IllegalArgumentException("empty messages"))

            val url = "$root/v1/chat/completions"
            val body = JSONObject()
            body.put("model", model.trim().ifEmpty { "hermes-agent" })
            body.put("stream", false)
            val arr = JSONArray()
            for ((role, content) in messages) {
                arr.put(
                    JSONObject().put("role", role).put("content", content),
                )
            }
            body.put("messages", arr)

            val reqBody = body.toString().toRequestBody(JSON_MEDIA)
            val builder = Request.Builder().url(url).post(reqBody)
            val key = apiKey.trim()
            if (key.isNotEmpty()) {
                builder.header("Authorization", "Bearer $key")
            }

            httpClient.newCall(builder.build()).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val detail = parseErrorMessage(bodyStr) ?: bodyStr.ifEmpty { "HTTP ${resp.code}" }
                    return Result.failure(RuntimeException(detail))
                }
                val json = JSONObject(bodyStr)
                val choices = json.optJSONArray("choices") ?: return Result.failure(
                    RuntimeException("invalid response: missing choices"),
                )
                val first = choices.optJSONObject(0) ?: return Result.failure(
                    RuntimeException("invalid response: empty choices"),
                )
                val message = first.optJSONObject("message") ?: return Result.failure(
                    RuntimeException("invalid response: missing message"),
                )
                val content = message.optString("content", "").trim()
                if (content.isEmpty()) {
                    return Result.failure(RuntimeException("invalid response: empty content"))
                }
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
