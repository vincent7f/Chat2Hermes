package com.herdroid.app.domain.tts

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.herdroid.app.domain.MessageSanitizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 使用系统 [TextToSpeech] 朗读文本。
 * 在引擎 [onInit] 完成前收到的 [speak] 会暂存，就绪后自动播放（避免首条回复被静默丢弃）。
 */
class SystemTtsSpeaker(app: Application) {

    private val appContext = app.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)

    @Volatile
    private var pendingPlain: String? = null

    init {
        mainHandler.post {
            tts = TextToSpeech(appContext) { status ->
                mainHandler.post {
                    if (status != TextToSpeech.SUCCESS) {
                        return@post
                    }
                    val engine = tts ?: return@post
                    configureLanguage(engine)
                    ready.set(true)
                    val queued = pendingPlain
                    pendingPlain = null
                    if (!queued.isNullOrEmpty()) {
                        speakNow(engine, queued)
                    }
                }
            }
        }
    }

    fun speak(text: String) {
        val plain = MessageSanitizer.forSpeech(text).take(MAX_TTS_CHARS)
        if (plain.isEmpty()) return
        mainHandler.post {
            val engine = tts
            if (!ready.get() || engine == null) {
                pendingPlain = plain
                return@post
            }
            speakNow(engine, plain)
        }
    }

    private fun speakNow(engine: TextToSpeech, plain: String) {
        val id = "herdroid-${System.nanoTime()}"
        engine.speak(plain, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun configureLanguage(engine: TextToSpeech) {
        var r = engine.setLanguage(Locale.getDefault())
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
        }
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
    }

    fun shutdown() {
        mainHandler.post {
            pendingPlain = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready.set(false)
        }
    }

    companion object {
        private const val MAX_TTS_CHARS = 4000
    }
}
