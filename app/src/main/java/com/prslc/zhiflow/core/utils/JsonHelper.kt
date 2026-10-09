package com.prslc.zhiflow.core.utils

import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.data.model.content.CardExtraInfo

/**
 * A utility object providing centralized JSON handling for Zhihu-specific data formats.
 */
object JsonHelper {

    private val json = HttpClientProvider.jsonInstance

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
     * @param data The object to serialize.
     * @return A JSON formatted string.
     */
    fun <T> toJson(data: T): String = json.encodeToString(data as Any)
}
