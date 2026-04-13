package com.prslc.zhiflow.core.utils

import com.prslc.zhiflow.data.model.CardExtraInfo
import kotlinx.serialization.json.Json

object JsonHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parseExtraInfo(jsonStr: String?): CardExtraInfo? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            json.decodeFromString<CardExtraInfo>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun cleanHtmlDesc(rawDesc: String?): String? {
        if (rawDesc.isNullOrBlank()) return null
        return android.text.Html.fromHtml(rawDesc, android.text.Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("回答|文章|视频|专栏"), "")
            .trim()
    }
}