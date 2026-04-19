package com.herdroid.app.domain.tts

import com.herdroid.app.domain.MessageSanitizer
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** 使用系统 [TextToSpeech] 朗读文本；须在主线程调用 [speak]。 */
class SystemTtsSpeaker(app: Application) {

    private val appContext = app.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)

    init {
        mainHandler.post {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    ready.set(true)
                }
            }
        }
    }

    fun speak(text: String) {
        val plain = MessageSanitizer.forSpeech(text)
        if (plain.isEmpty()) return
        mainHandler.post {
            val engine = tts ?: return@post
            if (!ready.get()) return@post
            val chunk = plain.take(MAX_TTS_CHARS)
            engine.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, "herdroid-utterance")
        }
    }

    fun shutdown() {
        mainHandler.post {
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
