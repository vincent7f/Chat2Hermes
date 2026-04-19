package com.herdroid.app.ui.hermes

import android.app.Application
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatFromSettings

/** [OpenAiChatFromSettings.PrepareOutcome] 非 Ready 时与主界面 / 设置页共用的提示文案。 */
fun OpenAiChatFromSettings.PrepareOutcome.errorMessage(application: Application): String? =
    when (this) {
        is OpenAiChatFromSettings.PrepareOutcome.Ready -> null
        is OpenAiChatFromSettings.PrepareOutcome.PortInvalid ->
            application.getString(R.string.test_feedback_port_invalid)
        is OpenAiChatFromSettings.PrepareOutcome.BaseUrlInvalid ->
            application.getString(R.string.chat_need_base_url)
        is OpenAiChatFromSettings.PrepareOutcome.ApiKeyMissing ->
            application.getString(R.string.chat_need_api_key)
    }
