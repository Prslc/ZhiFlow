package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Fetch recommended feed from Zhihu (supports pagination and refresh)
 *
 * @param isRefresh If true, force refresh and fetch the first page
 * @param nextUrl URL for the next page; if null or isRefresh is true, fetch the first page
 * @return ZhihuResponse containing feed data, or null if the request fails
 */
suspend fun getRecommendFeed(
    isRefresh: Boolean = false,
    nextUrl: String? = null
): ZhihuResponse? {
    val tag = "feedService"
    return try {
        val requestUrl = nextUrl.takeUnless { isRefresh || nextUrl == null } ?: "topstory/recommend"

        // Force refresh
        val response = Client.client.get(requestUrl) {
            if (isRefresh || nextUrl == null) {
                parameter("action", "down")
                parameter("start_type", "cold")
                parameter("limit", 10)
                parameter("is_feed_first_request", "1")
                parameter("refresh_scene", "1")
            }
        }

        response.body<ZhihuResponse>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch feed from $nextUrl", e)
        null
    }
}
