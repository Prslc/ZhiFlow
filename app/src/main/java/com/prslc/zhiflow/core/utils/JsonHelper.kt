package com.prslc.zhiflow.core.utils

import android.text.Html
import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.data.model.CardExtraInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * A utility object providing centralized JSON serialization and
 * specialized string cleaning for Zhihu-specific data formats.
 */
object JsonHelper {

    private val json = Client.jsonInstance

    /**
     * Deserializes a JSON string specifically for Zhihu card extra information.
     *
     * @param jsonStr The raw JSON string from the API.
     * @return A [CardExtraInfo] object or null if the string is blank or invalid.
     */
    fun parseExtraInfo(jsonStr: String?): CardExtraInfo? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            json.decodeFromString<CardExtraInfo>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a data object into its JSON string representation.
     * * @param data The object to serialize.
     * @return A JSON formatted string.
     */
    fun <T> toJson(data: T): String = json.encodeToString(data as Any)

    /**
     * Removes HTML tags and boilerplate category labels from a raw description string.
     *
     * This method uses Android's [Html] utility to strip tags and then removes
     * specific Zhihu keywords like "回答" (Answer) or "文章" (Article).
     *
     * @param rawDesc The string containing HTML or boilerplate text.
     * @return A cleaned, plain-text string, or null if input is empty.
     */
    fun cleanHtmlDesc(rawDesc: String?): String? {
        if (rawDesc.isNullOrBlank()) return null
        return Html.fromHtml(rawDesc, Html.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("回答|文章|视频|专栏"), "")
            .trim()
    }
}