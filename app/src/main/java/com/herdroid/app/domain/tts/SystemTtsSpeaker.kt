package com.herdroid.app.domain.tts

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.herdroid.app.domain.MessageSanitizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors

/**
 * 使用系统 [TextToSpeech] 朗读文本。
 *
 * **线程**：文本清洗、分段等准备逻辑在 [prepExecutor] 后台线程执行；[TextToSpeech] 的构造与
 * [TextToSpeech.speak] 仍在主线程执行（系统 API 要求），主线程仅做最短路径调用。
 * 在引擎 [onInit] 完成前收到的 [speak] 会暂存，就绪后自动播放。
 */
class SystemTtsSpeaker(app: Application) {

    private val appContext = app.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prepExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "herdroid-tts-prep").apply { isDaemon = true }
    }

    private var tts: TextToSpeech? = null
    private val ready = AtomicBoolean(false)

    @Volatile
    private var pendingPlain: String? = null

    private val lyricLines = mutableListOf<String>()
    private val lyricTotal = AtomicInteger(0)
    private val lyricCurrentIndex = AtomicInteger(-1)
    private var lyricCallback: LyricPlaybackCallback? = null
    private var lyricPaused = AtomicBoolean(false)

    init {
        mainHandler.post {
            tts = TextToSpeech(appContext) { status ->
                mainHandler.post {
                    if (status != TextToSpeech.SUCCESS) {
                        return@post
                    }
                    val engine = tts ?: return@post
                    configureLanguage(engine)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            val idx = utteranceId?.toIntOrNull() ?: return
                            lyricCurrentIndex.set(idx)
                            lyricCallback?.onLineWindow(windowForIndex(idx))
                        }

                        override fun onDone(utteranceId: String?) {
                            val idx = utteranceId?.toIntOrNull() ?: return
                            val total = lyricTotal.get()
                            if (idx == total - 1) {
                                mainHandler.post { finishLyricPlayback() }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post { handleUtteranceError("utterance error") }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post { handleUtteranceError("utterance error $errorCode") }
                        }
                    })
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

    /** 普通整段朗读（清洗在后台线程）。 */
    fun speak(text: String) {
        prepExecutor.execute {
            val plain = MessageSanitizer.forSpeech(text).take(MAX_TTS_CHARS)
            if (plain.isEmpty()) return@execute
            mainHandler.post {
                if (lyricCallback != null) {
                    stopLyricPlayback()
                }
                val engine = tts
                if (!ready.get() || engine == null) {
                    pendingPlain = plain
                    return@post
                }
                speakNow(engine, plain)
            }
        }
    }

    /**
     * 按行/段朗读并回调当前三行窗口（歌词式 UI）。
     * 准备与分段在 [prepExecutor]；排队 [speak] 在主线程。
     */
    fun speakWithLyrics(text: String, callback: LyricPlaybackCallback) {
        prepExecutor.execute {
            val plain = MessageSanitizer.forSpeech(text).take(MAX_TTS_CHARS)
            if (plain.isEmpty()) {
                mainHandler.post { callback.onError("empty") }
                return@execute
            }
            val lines = splitLinesForLyrics(plain)
            if (lines.isEmpty()) {
                mainHandler.post { callback.onError("empty") }
                return@execute
            }
            mainHandler.post {
                val engine = tts
                if (!ready.get() || engine == null) {
                    callback.onError("tts not ready")
                    return@post
                }
                engine.stop()
                lyricLines.clear()
                lyricLines.addAll(lines)
                lyricTotal.set(lines.size)
                lyricPaused.set(false)
                lyricCallback = callback
                lyricCurrentIndex.set(0)
                callback.onLineWindow(windowForIndex(0))
                lines.forEachIndexed { index, line ->
                    val params = Bundle()
                    val id = index.toString()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
                    val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                    engine.speak(line, queueMode, params, id)
                }
            }
        }
    }

    fun pauseLyricPlayback() {
        mainHandler.post {
            lyricPaused.set(true)
            tts?.stop()
        }
    }

    fun resumeLyricPlayback() {
        mainHandler.post {
            val engine = tts ?: return@post
            val cb = lyricCallback ?: return@post
            if (!lyricPaused.get()) return@post
            lyricPaused.set(false)
            val start = lyricCurrentIndex.get().coerceIn(0, lyricLines.lastIndex.coerceAtLeast(0))
            if (start >= lyricLines.size) {
                finishLyricPlayback()
                return@post
            }
            lyricLines.drop(start).forEachIndexed { i, line ->
                val index = start + i
                val params = Bundle()
                val id = index.toString()
                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
                val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                engine.speak(line, queueMode, params, id)
            }
        }
    }

    fun stopLyricPlayback() {
        mainHandler.post {
            tts?.stop()
            clearLyricState()
        }
    }

    private fun handleUtteranceError(message: String) {
        if (lyricPaused.get()) return
        lyricCallback?.onError(message)
        clearLyricState()
    }

    private fun finishLyricPlayback() {
        lyricCallback?.onComplete()
        clearLyricState()
    }

    private fun clearLyricState() {
        lyricLines.clear()
        lyricTotal.set(0)
        lyricCurrentIndex.set(-1)
        lyricCallback = null
        lyricPaused.set(false)
    }

    private fun windowForIndex(index: Int): LyricLineWindow {
        val lines = lyricLines
        if (lines.isEmpty()) return LyricLineWindow("", "", "", 0, 0)
        val prev = lines.getOrNull(index - 1).orEmpty()
        val curr = lines.getOrNull(index).orEmpty()
        val next = lines.getOrNull(index + 1).orEmpty()
        return LyricLineWindow(prev, curr, next, index, lines.size)
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
        prepExecutor.shutdown()
        mainHandler.post {
            pendingPlain = null
            lyricCallback = null
            lyricLines.clear()
            tts?.stop()
            tts?.shutdown()
            tts = null
            ready.set(false)
        }
    }

    companion object {
        private const val MAX_TTS_CHARS = 4000
        private const val MAX_LINE_CHARS = 120

        fun splitLinesForLyrics(plain: String): List<String> {
            val byNewline = plain.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            val chunks = if (byNewline.isNotEmpty()) {
                byNewline.flatMap { chunkLongLine(it) }
            } else {
                chunkLongLine(plain.trim())
            }
            return chunks.filter { it.isNotEmpty() }
        }

        private fun chunkLongLine(line: String): List<String> {
            if (line.length <= MAX_LINE_CHARS) return listOf(line)
            val out = mutableListOf<String>()
            var rest = line
            while (rest.isNotEmpty()) {
                if (rest.length <= MAX_LINE_CHARS) {
                    out.add(rest)
                    break
                }
                val breakAt = rest.lastIndexOfAny(charArrayOf('。', '！', '？', '，', '.', '!', '?', ','), MAX_LINE_CHARS)
                    .takeIf { it > 20 } ?: MAX_LINE_CHARS
                out.add(rest.take(breakAt + 1).trim())
                rest = rest.drop(breakAt + 1).trim()
            }
            return out
        }
    }
}

data class LyricLineWindow(
    val previous: String,
    val current: String,
    val next: String,
    val index: Int,
    val total: Int,
)

interface LyricPlaybackCallback {
    fun onLineWindow(window: LyricLineWindow)
    fun onComplete() {}
    fun onError(message: String) {}
}
