package com.prslc.zhiflow.data.remote.parser

import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.data.model.content.CardExtraInfo
import com.prslc.zhiflow.data.model.content.ColumnCardExtra

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
     * Deserializes the `extra_info` of the card a body ends with when it belongs to a column.
     *
     * @param jsonStr The raw JSON string from the API.
     * @return A [ColumnCardExtra] object or null if the string is blank or invalid.
     */
    fun parseColumnCard(jsonStr: String?): ColumnCardExtra? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ColumnCardExtra>(jsonStr)
        } catch (_: Exception) {
            null
        }
    }
}
