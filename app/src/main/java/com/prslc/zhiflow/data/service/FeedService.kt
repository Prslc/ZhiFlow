package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

suspend fun getRecommendFeed(limit: Int = 10, nextUrl: String? = null): ZhihuResponse? {
    val tag = "feedService"
    return try {
        val requestUrl = nextUrl ?: "topstory/recommend"

        val response = Client.client.get(requestUrl) {
            if (nextUrl == null) {
                parameter("limit", limit)
                parameter("action", "down")
            }
        }

        Log.d(tag, "Requesting: $requestUrl | Status: ${response.status}")

        response.body<ZhihuResponse>()
    } catch (e: Exception) {
        Log.e(tag, "Network Error", e)
        null
    }
}
