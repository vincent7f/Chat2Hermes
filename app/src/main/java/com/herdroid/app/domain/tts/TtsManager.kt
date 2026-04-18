package com.herdroid.app.domain.tts

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.herdroid.app.data.settings.TtsEngineType
import com.herdroid.app.domain.MessageSanitizer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class TtsManager(
    context: Context,
    private val httpClient: OkHttpClient,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private val ttsReady = AtomicBoolean(false)
    private val mediaPlayer = MediaPlayer()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady.set(true)
            }
        }
    }

    /**
     * New message interrupts current playback (PRD default).
     */
    fun speak(
        rawText: String,
        engine: TtsEngineType,
        networkBaseUrl: String,
        networkApiKey: String,
        onNetworkError: (String) -> Unit,
    ) {
        val text = MessageSanitizer.forSpeech(rawText)
        if (text.isEmpty()) return
        stopPlayback()
        when (engine) {
            TtsEngineType.SYSTEM -> mainHandler.post { speakSystem(text) }
            TtsEngineType.NETWORK -> {
                Thread {
                    val ok = tryNetworkTts(text, networkBaseUrl, networkApiKey)
                    if (!ok) {
                        mainHandler.post {
                            onNetworkError("Network TTS failed; using system TTS.")
                            speakSystem(text)
                        }
                    }
                }.start()
            }
        }
    }

    private fun speakSystem(text: String) {
        val engine = tts ?: return
        if (!ttsReady.get()) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "herdroid")
    }

    private fun tryNetworkTts(text: String, baseUrl: String, apiKey: String): Boolean {
        val root = baseUrl.trim().trimEnd('/')
        if (root.isEmpty()) return false
        val url = "$root/tts"
        val json = JSONObject().put("text", text).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val builder = Request.Builder().url(url).post(body)
        if (apiKey.isNotEmpty()) {
            builder.header("Authorization", "Bearer $apiKey")
        }
        val response = httpClient.newCall(builder.build()).execute()
        response.use { resp ->
            if (!resp.isSuccessful) return false
            val bytes = resp.body?.bytes() ?: return false
            if (bytes.isEmpty()) return false
            val file = File.createTempFile("herdroid-tts", ".audio", appContext.cacheDir)
            file.writeBytes(bytes)
            val latch = CountDownLatch(1)
            val result = booleanArrayOf(false)
            mainHandler.post {
                try {
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(file.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    result[0] = true
                } catch (_: Exception) {
                    result[0] = false
                } finally {
                    latch.countDown()
                }
            }
            latch.await()
            return result[0]
        }
    }

    /**
     * POST [sampleText] to `{baseUrl}/tts`, play audio on success. [onResult] on main thread.
     */
    fun testNetworkTts(
        networkBaseUrl: String,
        networkApiKey: String,
        sampleText: String,
        onResult: (success: Boolean, detail: String) -> Unit,
    ) {
        Thread {
            try {
                val ok = tryNetworkTts(sampleText, networkBaseUrl, networkApiKey)
                mainHandler.post {
                    onResult(
                        ok,
                        if (ok) {
                            "ok"
                        } else {
                            "play_or_http_failed"
                        },
                    )
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult(false, e.message ?: e.javaClass.simpleName)
                }
            }
        }.start()
    }

    fun stopPlayback() {
        runCatching {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
        tts?.stop()
    }

    fun release() {
        stopPlayback()
        runCatching { mediaPlayer.release() }
        tts?.shutdown()
        tts = null
    }
}
