package com.herdroid.app.domain

import android.content.Context
import android.media.AudioManager

/** 媒体音量（与系统 TTS 常用 [AudioManager.STREAM_MUSIC] 一致）。 */
object MediaVolumeChecker {

    /** 当前音量低于最大值的 50% 时视为过小（朗读前提示用户）。 */
    private const val HALF_PERCENT = 50

    fun isMediaVolumeBelowHalf(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return false
        return current * 100 / max < HALF_PERCENT
    }
}
