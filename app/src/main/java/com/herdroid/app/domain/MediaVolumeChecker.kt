package com.herdroid.app.domain

import android.content.Context
import android.media.AudioManager

/** 媒体音量（与系统 TTS 常用流一致）是否过低。 */
object MediaVolumeChecker {

    /** 当前音量低于最大值的该比例时视为过小（例如 12 表示 12%）。 */
    private const val LOW_PERCENT_THRESHOLD = 12

    fun isMediaVolumeTooLow(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return false
        return current * 100 / max < LOW_PERCENT_THRESHOLD
    }
}
