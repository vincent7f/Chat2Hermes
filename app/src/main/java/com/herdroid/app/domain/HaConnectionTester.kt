package com.herdroid.app.domain

import okhttp3.OkHttpClient
import okhttp3.Request

/** HA 连接测试：当前仅提供 HTTP 健康检查。 */
object HaConnectionTester {

    /**
     * HTTP GET（如 `{base}/health`），响应码 2xx 视为成功。
     */
    fun testHttpGet(client: OkHttpClient, url: String, bearerToken: String = ""): Result<Unit> {
        return try {
            val rb = Request.Builder().url(url).get()
            val key = bearerToken.trim()
            if (key.isNotEmpty()) {
                rb.header("Authorization", "Bearer $key")
            }
            client.newCall(rb.build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
